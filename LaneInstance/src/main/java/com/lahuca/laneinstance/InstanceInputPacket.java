package com.lahuca.laneinstance;

import com.github.benmanes.caffeine.cache.Cache;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.packet.GameShutdownRequestPacket;
import com.lahuca.lane.connection.packet.InstanceJoinPacket;
import com.lahuca.lane.connection.packet.InstanceUpdatePlayerPacket;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.result.VoidResultPacket;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lane.records.PartyRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.laneinstance.events.party.*;
import com.lahuca.laneinstance.game.InstanceGame;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class InstanceInputPacket implements Consumer<InputPacket> {

    private final LaneInstance instance;
    private final Cache<Long, InstanceParty> partyReplicas;

    public InstanceInputPacket(LaneInstance instance, Cache<Long, InstanceParty> partyReplicas) {
        this.instance = instance;
        this.partyReplicas = partyReplicas;
    }

    public InstancePlayerManager getPlayerManager() {
        return instance.getPlayerManager();
    }

    public Optional<InstanceGame> getInstanceGame(long gameId) {
        return instance.getInstanceGame(gameId);
    }

    private void sendSimpleResult(long requestId, String result) {
        instance.sendController(new VoidResultPacket(requestId, result));
    }

    private void sendSimpleResult(RequestPacket request, String result) {
        sendSimpleResult(request.getRequestId(), result);
    }

    public CompletableFuture<Void> unregisterGame(long gameId) {
        return instance.unregisterGame(gameId);
    }

    public <E extends LaneEvent> CompletableFuture<E> handleInstanceEvent(E event) {
        return instance.handleInstanceEvent(event);
    }

    public void disconnectPlayer(UUID player, Component reason) {
        instance.disconnectPlayer(player, reason);
    }

    public ReconnectConnection getConnection() {
        return instance.getConnection();
    }

    @Override
    public void accept(InputPacket input) {
        switch (input.packet()) {
            case InstanceJoinPacket packet -> {
                // TODO This all assumes we do all party members one at a time!!
                // Check if we can even join that queue type
                HashSet<LanePlayer> kickable = null;
                Map<UUID, Integer> playerMap = Map.of(packet.player().uuid(), packet.player().queuePriority());
                Set<UUID> playerSet = Set.of(packet.player().uuid());
                if (!getPlayerManager().hasQueueSlots(playerSet, packet.queueType())) {
                    // Cannot grant slot yet, check if we even can join
                    if (!getPlayerManager().isQueueJoinable(packet.queueType())) {
                        // We cannot join the queue type
                        sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                        return;
                    }
                    // Okay, check if we can kick someone
                    kickable = getPlayerManager().findKickableLanePlayers(playerMap, packet.queueType(), packet.gameId(), getPlayerManager()::getInstancePlayer);
                    if (kickable == null) {
                        // We could not find a slot
                        sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                        return;
                    }
                    // Okay so we can kick the given player to join the instance (and game if present). First wait for game checks.
                }
                if (packet.gameId() != null) {
                    // Do the same for the game
                    Optional<InstanceGame> instanceGame = getInstanceGame(packet.gameId());
                    if (instanceGame.isEmpty()) {
                        sendSimpleResult(packet, ResponsePacket.INVALID_ID);
                        return;
                    }
                    InstanceGame game = instanceGame.get();
                    // Check if we can even join that queue type
                    if (!game.hasQueueSlots(playerSet, packet.queueType())) {
                        // Cannot grant slot yet, check if we even can join
                        if (!game.isQueueJoinable(packet.queueType())) {
                            // We cannot join the queue type
                            sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                            return;
                        }
                        // Okay, so we need to kick someone
                        if (kickable == null) {
                            // So we kick someone
                            kickable = getPlayerManager().findKickableLanePlayers(playerMap, packet.queueType(), packet.gameId(), getPlayerManager()::getInstancePlayer);
                            if (kickable == null) {
                                // We could not find a slot
                                sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                                return;
                            }
                        }
                        // So if there was already someone found, they will be kicked
                    }
                }
                if (kickable != null) {
                    // Kick the player, we can proceed basically
                    kickable.forEach(kick -> {
                        disconnectPlayer(kick.getUuid(), Component.translatable("queue.kick.lowPriority")); // TODO Translate!!
                    });
                }
                // We are here, so we can apply it.
                getPlayerManager().registerPlayer(packet.player(), new InstancePlayer.RegisterData(packet.queueType(), packet.parameter(), packet.gameId()));
                sendSimpleResult(packet, ResponsePacket.OK);
            }
            case InstanceUpdatePlayerPacket packet -> {
                PlayerRecord record = packet.playerRecord();
                getPlayerManager().getInstancePlayer(record.uuid()).ifPresent(player -> player.applyRecord(record));
            }
            case GameShutdownRequestPacket(long requestId, long gameId) ->
                    unregisterGame(gameId).whenComplete((data, ex) -> {
                        if (ex != null) {
                            // TODO Add more exceptions. To write and remove as well!
                            String result = switch (ex) {
                                case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                                case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                                default -> ResponsePacket.UNKNOWN;
                            };
                            sendSimpleResult(requestId, result);
                        } else {
                            sendSimpleResult(requestId, ResponsePacket.OK);
                        }
                    });
            case PartyPacket.Event packet -> {
                InstanceParty party = partyReplicas.getIfPresent(packet.partyId());
                if (party != null) {
                    switch (packet) {
                        case PartyPacket.Event.AcceptInvitation(
                                long partyId, UUID player, PartyRecord value
                        ) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartyAcceptInvitationEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.AddInvitation(
                                long partyId, UUID invited, PartyRecord value
                        ) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(invited).ifPresent(current -> {
                                handleInstanceEvent(new PartyAddInvitationEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.Create(long partyId, UUID player, PartyRecord value) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartyCreateEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.DenyInvitation(
                                long partyId, UUID player, PartyRecord value
                        ) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartyDenyInvitationEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.Disband(long partyId) -> {
                            party.removeReplicated();
                            partyReplicas.invalidate(partyId);
                            handleInstanceEvent(new PartyDisbandEvent(party));
                        }
                        case PartyPacket.Event.JoinPlayer(long partyId, UUID player, PartyRecord value) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartyJoinPlayerEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.RemovePlayer(
                                long partyId, UUID player, PartyRecord value
                        ) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartyRemovePlayerEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.SetInvitationsOnly(
                                long partyId, boolean invitationsOnly, PartyRecord value
                        ) -> {
                            party.applyRecord(value);
                            handleInstanceEvent(new PartySetInvitationsOnlyEvent(party, invitationsOnly));

                        }
                        case PartyPacket.Event.SetOwner(long partyId, UUID player, PartyRecord value) -> {
                            party.applyRecord(value);
                            getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                                handleInstanceEvent(new PartySetOwnerEvent(party, current)); // TODO Only when player is online?
                            });
                        }
                        case PartyPacket.Event.Warp(long partyId) -> {
                            handleInstanceEvent(new PartyWarpEvent(party));
                        }

                        default -> throw new IllegalStateException("Unexpected value: " + packet);
                    }
                    ;
                } else {
                    if (packet instanceof PartyPacket.Event.Create(
                            long partyId, UUID player, PartyRecord value
                    )) {
                        InstanceParty newParty = new InstanceParty(value);
                        partyReplicas.put(partyId, newParty);
                        getPlayerManager().getInstancePlayer(player).ifPresent(current -> {
                            handleInstanceEvent(new PartyCreateEvent(newParty, current)); // TODO Only when player is online?
                        });
                    }
                }
            }
            case ResponsePacket<?> response -> {
                if (!getConnection().retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
                    // TODO Handle output: failed response
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + input.packet());
        }

    }

}
