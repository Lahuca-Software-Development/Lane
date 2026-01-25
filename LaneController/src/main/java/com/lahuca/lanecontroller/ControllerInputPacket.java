package com.lahuca.lanecontroller;

import com.lahuca.lane.FriendshipInvitation;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.InputPacket;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.data.*;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.connection.request.result.*;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.queue.QueueRequestReason;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RelationshipRecord;
import com.lahuca.lanecontroller.events.InstanceRegisterEvent;
import com.lahuca.lanecontroller.events.QueueFinishedEvent;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ControllerInputPacket implements Consumer<InputPacket> {

    private final Controller controller;
    private final DataManager dataManager;

    private final HashMap<Long, ControllerGame> games; // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances; // Additional data for the instances

    public ControllerInputPacket(Controller controller,
                                 DataManager dataManager,
                                 HashMap<Long, ControllerGame> games,
                                 HashMap<String, ControllerLaneInstance> instances) {
        this.controller = controller;
        this.dataManager = dataManager;
        this.games = games;
        this.instances = instances;
    }

    public static Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return Controller.getPlayer(uuid);
    }

    public Connection getConnection() {
        return controller.getConnection();
    }

    public Optional<ControllerGame> getGame(long id) {
        return controller.getGame(id);
    }

    public ControllerDataManager getDataManager() {
        return controller.getDataManager();
    }

    public void sendMessage(UUID player, Component message) {
        controller.sendMessage(player, message);
    }

    public ControllerPlayerManager getPlayerManager() {
        return controller.getPlayerManager();
    }

    public ControllerPartyManager getPartyManager() {
        return controller.getPartyManager();
    }

    public ControllerFriendshipManager getFriendshipManager() {
        return controller.getFriendshipManager();
    }

    public Optional<ControllerLaneInstance> getInstance(String id) {
        return controller.getInstance(id);
    }

    public <E extends LaneEvent> CompletableFuture<E> handleControllerEvent(E event) {
        return controller.handleControllerEvent(event);
    }

    public void setEffectiveLocale(UUID player, Locale locale) {
        controller.setEffectiveLocale(player, locale);
    }

    @Override
    public void accept(InputPacket input) {
        Packet iPacket = input.packet();
        switch (iPacket) {
            case GameStatusUpdatePacket(long requestId, GameRecord record) -> {
                long gameId = record.gameId();
                if (!input.from().equals(record.instanceId())) {
                    getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                    return;
                }
                if (!games.containsKey(gameId)) {
                    // A new game has been created, yeey!
                    games.put(gameId, new ControllerGame(record));
                    getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                    return;
                }
                ControllerGame game = games.get(gameId);
                if (!game.getInstanceId().equals(input.from())) {
                    getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                    return;
                }
                games.get(gameId).applyRecord(record);
                getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
            }
            case GameShutdownPacket(long requestId, long gameId) -> {
                ControllerGame game = games.get(gameId);
                if (game == null) {
                    getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from());
                    return;
                }
                if (!input.from().equals(game.getInstanceId())) {
                    getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                    return;
                }
                // Remove the game
                games.remove(gameId);
                // Update queue
                game.getOnline().forEach(uuid -> getPlayer(uuid).ifPresent(player -> {
                    player.setGameId(null);
                    if (player.getQueueRequest().isEmpty()) {
                        // We do not have a queue yet, requeue for a new server
                        // We NEED one, so do not allow none
                        player.queue(new QueueRequest(QueueRequestReason.GAME_SHUTDOWN, null, QueueRequestParameters.lobbyParameters), false);
                    }
                }));
                getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
            }
            case GameQuitPacket(long requestId, UUID uuid) ->
                    getPlayerManager().getPlayer(uuid).ifPresentOrElse(player -> {
                        player.getGameId().flatMap(this::getGame).ifPresentOrElse(game -> {
                            if (!input.from().equals(game.getInstanceId())) {
                                getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                                return;
                            }
                            // Okay we can quit the game
                            player.setGameId(null);
                            if (player.getQueueRequest().isEmpty()) {
                                // We do not have a queue yet, requeue for a new server
                                // We NEED one, so do not allow none
                                player.queue(new QueueRequest(QueueRequestReason.GAME_QUIT, null, QueueRequestParameters.lobbyParameters), false);
                            }
                            getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                        }, () -> getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from()));
                    }, () -> getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from()));
            case InstanceStatusUpdatePacket(InstanceRecord record) -> {
                if (!input.from().equals(record.id())) {
                    // TODO Report?
                    return;
                }
                if (!instances.containsKey(record.id())) {
                    instances.put(record.id(), new ControllerLaneInstance(record));
                    handleControllerEvent(new InstanceRegisterEvent(instances.get(record.id())));
                    return;
                }
                instances.get(record.id()).applyRecord(record);
            }

            case PartyPacket.Retrieve.Request packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(
                            party -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.OK, party.convertRecord()), input.from()),
                            () -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Retrieve.RequestPlayerParty packet ->
                    getPlayer(packet.player()).ifPresentOrElse(player -> player.getParty().ifPresentOrElse(party -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.OK, party.convertRecord()), input.from()), () -> {
                        if (!packet.createIfNeeded()) {
                            getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                            return;
                        }
                        getPartyManager().createParty(player).ifPresentOrElse(newParty -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.OK, newParty.convertRecord()), input.from()), () -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from()));
                    }), () -> getConnection().sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Retrieve.Subscribe packet -> getPartyManager().getParty(packet.partyId()).ifPresent(
                    party -> party.subscribeReplicated(input.from()));
            case PartyPacket.Retrieve.Unsubscribe packet -> getPartyManager().getParty(packet.partyId()).ifPresent(
                    party -> party.unsubscribeReplicated(input.from()));
            case PartyPacket.Operations.AcceptInvitation packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.acceptInvitation(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.AddInvitation packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.addInvitation(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.Create packet ->
                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player -> {
                        if (player.getParty().isPresent()) {
                            getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.requestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                            return;
                        }
                        getPartyManager().createParty(player).ifPresentOrElse(party ->
                                        getConnection().sendPacket(new SimpleResultPacket<>(packet.requestId(), ResponsePacket.OK, party.convertRecord()), input.from()),
                                () -> getConnection().sendPacket(new SimpleResultPacket<>(packet.requestId(), ResponsePacket.ILLEGAL_STATE), input.from()));
                    }, () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
            case PartyPacket.Operations.DenyInvitation packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.denyInvitation(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.Disband packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.disband()), input.from()),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.JoinPlayer packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.joinPlayer(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.RemovePlayer packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.removePlayer(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.SetInvitationsOnly packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party -> {
                        party.setInvitationsOnly(packet.invitationsOnly());
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    }, () -> getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.SetOwner packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.setOwner(player)), input.from()),
                                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            case PartyPacket.Operations.Warp packet ->
                    getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.warpParty()), input.from()),
                            () -> getConnection().sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));

            case QueueRequestPacket packet -> {
                if (packet.parameters() == null) {
                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), "requestParameters must not be null"), input.from());
                    return;
                }
                getPlayer(packet.player()).ifPresentOrElse(player -> player.queue(new QueueRequest(QueueRequestReason.PLUGIN_INSTANCE, packet.parameters()), true).whenComplete((result, exception) -> {
                    String response = ResponsePacket.OK;
                    if (exception != null) {
                        if (exception instanceof UnsuccessfulResultException ex) {
                            response = ex.getMessage();
                        } else {
                            response = ResponsePacket.UNKNOWN;
                        }
                    }
                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), response), input.from());
                }), () -> getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
            }
            case QueueFinishedPacket packet -> {
                getPlayer(packet.player()).ifPresentOrElse(player -> {
                    // Player should have finished its queue, check whether it is allowed.
                    player.getQueueRequest().ifPresentOrElse(queue -> {
                        // There is a queue, check if the state of the player was to transfer to the retrieved instance/game.
                        ControllerPlayerState state = player.getState();
                        if (state != null && state.getProperties() != null && state.getProperties().containsKey(LaneStateProperty.INSTANCE_ID)) {
                            // Check if we either joined the correct instance or game.
                            if (state.getName().equals(LanePlayerState.INSTANCE_TRANSFER) && state.getProperties().get(LaneStateProperty.INSTANCE_ID).getValue().equals(input.from())) {
                                // We joined an instance.
                                ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.INSTANCE_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                player.setState(newState);
                                player.setQueueRequest(null);
                                player.setGameId(null);
                                player.setInstanceId(input.from());
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                Controller.getInstance().handleControllerEvent(new QueueFinishedEvent(player, queue, input.from(), null));
                            } else if (packet.gameId() != null && state.getProperties().containsKey(LaneStateProperty.GAME_ID) && state.getProperties().get(LaneStateProperty.GAME_ID).getValue().equals(packet.gameId()) && state.getName().equals(LanePlayerState.GAME_TRANSFER)) {
                                // We joined a game.
                                ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.GAME_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.GAME_ID, packet.gameId()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                player.setState(newState);
                                player.setQueueRequest(null);
                                player.setGameId(packet.gameId());
                                player.setInstanceId(input.from());
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                Controller.getInstance().handleControllerEvent(new QueueFinishedEvent(player, queue, input.from(), packet.gameId()));
                            } else {
                                // We cannot accept this queue finalization.
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                            }
                            return;
                        }
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                    }, () -> getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from()));
                }, () -> getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
            }
            case RequestIdPacket packet -> {
                Long newId;
                switch (packet.type()) {
                    case GAME -> {
                        do {
                            newId = System.currentTimeMillis();
                        } while (games.containsKey(newId));
                    }
                    default -> newId = null;
                }
                if (newId == null) {
                    getConnection().sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                } else {
                    // TODO Do reservation, we do not want doubles!
                    getConnection().sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.OK, newId), input.from());
                }
            }
            case DataObjectReadPacket packet -> {
                if (!packet.permissionKey().isIndividual()) {
                    getConnection().sendPacket(new DataObjectResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.readDataObject(packet.permissionKey(), packet.id()).whenComplete((object, ex) -> {
                    if (ex != null) {
                        // TODO Add more exceptions. To write and remove as well!
                        String result = switch (ex) {
                            case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                            case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                            default -> ResponsePacket.UNKNOWN;
                        };
                        getConnection().sendPacket(new DataObjectResultPacket(packet.getRequestId(), result), input.from());
                    } else {
                        getConnection().sendPacket(new DataObjectResultPacket(packet.getRequestId(), ResponsePacket.OK, object.orElse(null)), input.from());
                    }
                });
            }
            case DataObjectWritePacket packet -> {
                if (!packet.permissionKey().isIndividual()) {
                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.writeDataObject(packet.permissionKey(), packet.object()).whenComplete((bool, ex) -> {
                    if (ex != null) {
                        String result = switch (ex) {
                            case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                            case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                            case IllegalStateException ignored -> ResponsePacket.ILLEGAL_STATE;
                            case SecurityException ignored -> ResponsePacket.ILLEGAL_STATE;
                            default -> ResponsePacket.UNKNOWN;
                        };
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                    } else {
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    }
                });
            }
            case DataObjectRemovePacket packet -> {
                if (!packet.permissionKey().isIndividual()) {
                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.removeDataObject(packet.permissionKey(), packet.id()).whenComplete((bool, ex) -> {
                    if (ex != null) {
                        String result = switch (ex) {
                            case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                            case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                            case IllegalStateException ignored -> ResponsePacket.ILLEGAL_STATE;
                            case SecurityException ignored -> ResponsePacket.ILLEGAL_STATE;
                            default -> ResponsePacket.UNKNOWN;
                        };
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                    } else {
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    }
                });
            }
            case DataObjectListIdsPacket packet -> {
                dataManager.listDataObjectIds(packet.prefix()).whenComplete((object, ex) -> {
                    if (ex != null) {
                        // TODO Add more exceptions. To write and remove as well!
                        String result = switch (ex) {
                            case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                            case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                            default -> ResponsePacket.UNKNOWN;
                        };
                        getConnection().sendPacket(new DataObjectIdsResultPacket(packet.getRequestId(), result), input.from());
                    } else {
                        getConnection().sendPacket(new DataObjectIdsResultPacket(packet.getRequestId(), ResponsePacket.OK, object), input.from());
                    }
                });
            }

            case DataObjectsListPacket packet -> {
                dataManager.listDataObjects(packet.prefix(), packet.permissionKey(), packet.version())
                        .whenComplete((object, ex) -> {
                            if (ex != null) {
                                // TODO Add more exceptions. To write and remove as well!
                                String result = switch (ex) {
                                    case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                                    case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                                    default -> ResponsePacket.UNKNOWN;
                                };
                                getConnection().sendPacket(new DataObjectsResultPacket(packet.getRequestId(), result), input.from());
                            } else {
                                getConnection().sendPacket(new DataObjectsResultPacket(packet.getRequestId(), ResponsePacket.OK, object), input.from());
                            }
                        });
            }

            case DataObjectCopyPacket packet -> {
                if (!packet.permissionKey().isIndividual()) {
                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.copyDataObject(packet.permissionKey(), packet.sourceId(), packet.targetId()).whenComplete((bool, ex) -> {
                    if (ex != null) {
                        String result = switch (ex) {
                            case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                            case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                            case IllegalStateException ignored -> ResponsePacket.ILLEGAL_STATE;
                            case SecurityException ignored -> ResponsePacket.ILLEGAL_STATE;
                            default -> ResponsePacket.UNKNOWN;
                        };
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                    } else {
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    }
                });
            }

            case RequestInformationPacket.Player packet ->
                    getConnection().sendPacket(new RequestInformationPacket.PlayerResponse(packet.getRequestId(), ResponsePacket.OK, getPlayer(packet.uuid()).map(ControllerPlayer::convertRecord).orElse(null)), input.from());
            case RequestInformationPacket.Players packet -> {
                ArrayList<PlayerRecord> data = new ArrayList<>();
                for (ControllerPlayer value : getPlayerManager().getPlayers()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                getConnection().sendPacket(new RequestInformationPacket.PlayersResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            }
            case RequestInformationPacket.Game packet ->
                    getConnection().sendPacket(new RequestInformationPacket.GameResponse(packet.getRequestId(), ResponsePacket.OK, getGame(packet.gameId()).map(ControllerGame::convertRecord).orElse(null)), input.from());
            case RequestInformationPacket.Games packet -> {
                ArrayList<GameRecord> data = new ArrayList<>();
                for (ControllerGame value : games.values()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                getConnection().sendPacket(new RequestInformationPacket.GamesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            }
            case RequestInformationPacket.Instance packet -> {
                getConnection().sendPacket(new RequestInformationPacket.InstanceResponse(packet.getRequestId(), ResponsePacket.OK, getInstance(packet.id()).map(ControllerLaneInstance::convertRecord).orElse(null)), input.from());
            }
            case RequestInformationPacket.Instances packet -> {
                ArrayList<InstanceRecord> data = new ArrayList<>();
                for (ControllerLaneInstance value : instances.values()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                getConnection().sendPacket(new RequestInformationPacket.InstancesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            }
            case RequestInformationPacket.PlayerUsername packet ->
                    getPlayerManager().getPlayerUsername(packet.uuid()).whenComplete((username, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, username.orElse(null)), input.from());
                    });
            case RequestInformationPacket.PlayerUuid packet ->
                    getPlayerManager().getPlayerUuid(packet.username()).whenComplete((uuid, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, uuid.orElse(null)), input.from());
                    });
            case RequestInformationPacket.PlayerNetworkProfile packet ->
                    getPlayerManager().getPlayerNetworkProfile(packet.uuid())
                            .whenComplete((data, ex) -> {
                                if (ex != null) {
                                    // TODO Additional instnceof? As read?
                                    if (ex instanceof IllegalArgumentException) {
                                        getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, data.map(ProfileData::convertRecord).orElse(null)), input.from());
                            });
            case SendMessagePacket packet -> sendMessage(packet.player(), packet.message());

            case SavedLocalePacket.Get packet -> {
                getDataManager().getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                        .thenCompose(profile -> getPlayerManager().getSavedLocale(profile))
                        .whenComplete((locale, ex) -> {
                            if (ex != null) {
                                // TODO Additional instnceof? As read?
                                if (ex instanceof IllegalArgumentException) {
                                    getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                    return;
                                }
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                return;
                            }
                            if (locale.isPresent()) {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, locale.get().toLanguageTag()), input.from());
                            } else {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, null), input.from());
                            }
                        });
            }
            case SavedLocalePacket.Set packet ->
                    getDataManager().getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(profile -> getPlayerManager().setSavedLocale(profile, Locale.forLanguageTag(packet.locale())).thenApply(data -> profile))
                            .whenComplete((profile, ex) -> {
                                if (ex != null) {
                                    // TODO Additional instnceof? As write?
                                    if (ex instanceof IllegalArgumentException) {
                                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                setEffectiveLocale(profile.getFirstSuperProfile(), Locale.forLanguageTag(packet.locale())); // TODO On evnet
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                            });

            case ProfilePacket profilePacket -> profilePacket(profilePacket, input);
            case SetInformationPacket setInformation -> {
                switch (setInformation) {
                    case SetInformationPacket.PlayerSetNickname(long requestId, UUID uuid, String nickname) ->
                            getPlayer(uuid).ifPresentOrElse(player -> player.setNickname(nickname).whenComplete((result, exception) -> {
                                String response = ResponsePacket.OK;
                                if (exception != null) {
                                    if (exception instanceof UnsuccessfulResultException ex) {
                                        response = ex.getMessage();
                                    } else {
                                        response = ResponsePacket.UNKNOWN;
                                    }
                                }
                                getConnection().sendPacket(new VoidResultPacket(requestId, response), input.from());
                            }), () -> getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_PLAYER), input.from()));
                    default -> throw new IllegalStateException("Unexpected value: " + setInformation);
                }
            }
            case FriendshipPacket friendshipPacket -> friendshipPacket(friendshipPacket, input);
            case ResponsePacket<?> response -> {
                if (!getConnection().retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
                    // TODO Well, log about packet that is not wanted.
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + iPacket);
        }
    }

    private void profilePacket(ProfilePacket profilePacket, InputPacket input) {
        switch (profilePacket) {
            case ProfilePacket.GetProfileData packet ->
                    getDataManager().getProfileData(packet.uuid()).whenComplete((opt, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, opt.map(ControllerProfileData::convertRecord).orElse(null)), input.from());
                    });
            case ProfilePacket.CreateNew packet ->
                    getDataManager().createNewProfile(packet.type()).whenComplete((profile, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                    });
            case ProfilePacket.CreateSubProfile packet ->
                    getDataManager().getProfileData(packet.current()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(current -> getDataManager().createSubProfile(current, packet.type(), packet.name(), packet.active()))
                            .whenComplete((profile, ex) -> {
                                if (ex != null) {
                                    // TODO Additional instnceof? As read?
                                    if (ex instanceof IllegalArgumentException) {
                                        getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                getConnection().sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                            });
            case ProfilePacket.AddSubProfile packet -> getDataManager().getProfileData(packet.current())
                    .thenApply(opt -> opt.orElse(null))
                    .thenCompose(current ->
                            getDataManager().getProfileData(packet.subProfile())
                                    .thenApply(opt2 -> opt2.orElse(null))
                                    .thenCompose(subProfile -> getDataManager().addSubProfile(current, subProfile, packet.name(), packet.active())))
                    .whenComplete((status, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                    });
            case ProfilePacket.RemoveSubProfile packet -> getDataManager().getProfileData(packet.current())
                    .thenApply(opt -> opt.orElse(null))
                    .thenCompose(current ->
                            getDataManager().getProfileData(packet.subProfile())
                                    .thenApply(opt2 -> opt2.orElse(null))
                                    .thenCompose(subProfile -> getDataManager().removeSubProfile(current, subProfile, packet.name())))
                    .whenComplete((status, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                    });
            case ProfilePacket.ResetDelete packet -> getDataManager().getProfileData(packet.current())
                    .thenApply(opt -> opt.orElse(null))
                    .thenCompose(current -> getDataManager().resetDeleteProfile(current, packet.delete()))
                    .whenComplete((status, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As write?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            if (ex instanceof IllegalStateException) {
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                                return;
                            }
                            getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    });
            case ProfilePacket.Copy packet -> getDataManager().getProfileData(packet.current())
                    .thenApply(opt -> opt.orElse(null))
                    .thenCompose(current ->
                            getDataManager().getProfileData(packet.from())
                                    .thenApply(opt2 -> opt2.orElse(null))
                                    .thenCompose(from -> getDataManager().copyProfile(current, from)))
                    .whenComplete((status, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As read?
                            if (ex instanceof IllegalArgumentException) {
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    });
            case ProfilePacket.SetNetworkProfile packet -> {
                ControllerPlayer player = getPlayer(packet.player()).orElse(null);
                getDataManager().getProfileData(packet.profile())
                        .thenApply(opt -> opt.orElse(null))
                        .thenCompose(profile -> getDataManager().setNetworkProfile(player, profile))
                        .whenComplete((status, ex) -> {
                            if (ex != null) {
                                // TODO Additional instnceof? As write?
                                if (ex instanceof IllegalArgumentException) {
                                    getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                    return;
                                }
                                getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                return;
                            }
                            getConnection().sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                        });
            }
            default -> throw new IllegalStateException("Unexpected value: " + profilePacket);
        }
    }

    private void friendshipPacket(FriendshipPacket friendshipPacket, InputPacket input) {
        switch (friendshipPacket) {
            case FriendshipPacket.GetInvitations(
                    long requestId, UUID uuid, Boolean includeRequester, Boolean includeInvited
            ) -> {
                if (uuid == null) {
                    getConnection().sendPacket(new FriendshipInvitationsPacket(requestId, getFriendshipManager().getInvitations().keySet().stream().toList()), input.from());
                    getConnection();
                }
                getPlayer(uuid).ifPresentOrElse(player ->
                                getConnection().sendPacket(new FriendshipInvitationsPacket(requestId, getFriendshipManager().getInvitations(player, includeRequester, includeInvited).keySet().stream().toList()), input.from()),
                        () -> getConnection().sendPacket(new FriendshipInvitationsPacket(requestId, ResponsePacket.INVALID_PLAYER), input.from()));
            }
            case FriendshipPacket.ContainsInvitation(long requestId, FriendshipInvitation invitation) ->
                    getConnection().sendPacket(new SimpleResultPacket<>(requestId, ResponsePacket.OK, getFriendshipManager().containsInvitation(invitation)), input.from());
            case FriendshipPacket.InvalidateInvitation(long requestId, FriendshipInvitation invitation) -> {
                getFriendshipManager().invalidateInvitation(invitation);
                getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
            }
            case FriendshipPacket.Invite(
                    long requestId, FriendshipInvitation invitation, String username
            ) -> {
                getFriendshipManager().invite(invitation, username);
                getConnection().sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
            }
            case FriendshipPacket.AcceptInvitation(long requestId, FriendshipInvitation invitation) ->
                    getFriendshipManager().acceptInvitation(invitation).whenComplete((result, exception) -> {
                        String response = ResponsePacket.OK;
                        if (exception != null) {
                            if (exception instanceof UnsuccessfulResultException ex) {
                                response = ex.getMessage();
                            } else {
                                response = ResponsePacket.UNKNOWN;
                            }
                        }
                        getConnection().sendPacket(new VoidResultPacket(requestId, response), input.from());
                    });
            case FriendshipPacket.GetFriendship(long requestId, long friendshipId) -> {
                getFriendshipManager().getFriendship(friendshipId).whenComplete((result, exception) -> {
                    String response = ResponsePacket.OK;
                    if (exception != null) {
                        if (exception instanceof UnsuccessfulResultException ex) {
                            response = ex.getMessage();
                        } else {
                            response = ResponsePacket.UNKNOWN;
                        }
                    }
                    getConnection().sendPacket(new SimpleResultPacket<>(requestId, response, result), input.from());
                });
            }
            case FriendshipPacket.GetFriendships(long requestId, UUID uuid) ->
                    getPlayer(uuid).ifPresentOrElse(player -> getFriendshipManager().getFriendships(player).whenComplete((result, exception) -> {
                        String response = ResponsePacket.OK;
                        if (exception != null) {
                            if (exception instanceof UnsuccessfulResultException ex) {
                                response = ex.getMessage();
                            } else {
                                response = ResponsePacket.UNKNOWN;
                            }
                        }
                        getConnection().sendPacket(new RelationshipRecordsPacket(requestId, response, result), input.from());
                    }), () -> getConnection().sendPacket(new RelationshipRecordsPacket(requestId, ResponsePacket.INVALID_PLAYER), input.from()));

            case FriendshipPacket.RemoveFriendship(long requestId, long friendshipId) -> {
                getFriendshipManager().getFriendship(friendshipId).whenComplete((friendship, ex) -> {
                    if (ex != null) {
                        getConnection().sendPacket(new SimpleResultPacket<>(requestId, ResponsePacket.INVALID_PARAMETERS), input.from());
                        ex.printStackTrace();
                        return;
                    }

                    if (friendship == null) {
                        getConnection().sendPacket(new SimpleResultPacket<>(requestId, ResponsePacket.INVALID_ID), input.from());
                        throw new IllegalStateException("Friendship does not exist");
                    }

                    //TODO do not create new RelationshipRecord? fix that, however the retrieved friendshipRecord does not contain the ID.
                    getFriendshipManager().removeFriendship(new RelationshipRecord(friendshipId, friendship.players())).whenComplete((result, exception) -> {
                        String response = ResponsePacket.OK;
                        if (exception != null) {
                            if (exception instanceof UnsuccessfulResultException e) {
                                response = e.getMessage();
                            } else {
                                response = ResponsePacket.UNKNOWN;
                            }
                        }
                        getConnection().sendPacket(new SimpleResultPacket<>(requestId, response, result), input.from());
                    });
                });
            }
            default -> throw new IllegalStateException("Unexpected value: " + friendshipPacket);
        }
    }

}
