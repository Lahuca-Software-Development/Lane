/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 13-3-2024 at 21:31 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.google.gson.Gson;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.data.*;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.connection.request.result.*;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.queue.QueueRequestReason;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lanecontroller.events.ControllerEvent;
import com.lahuca.lanecontroller.events.QueueFinishedEvent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public abstract class Controller {

    private static Controller instance;

    public static Controller getInstance() {
        return instance;
    }

    /**
     * Retrieves the player associated with the given UUID statically from the controller.
     * It is preferred to use the {@link ControllerPlayerManager#getPlayer(UUID)} method on an instance instead.
     * If the player is not found, an empty {@link Optional} is returned.
     *
     * @param uuid the unique identifier of the player
     * @return an {@link Optional} containing the {@link ControllerPlayer} if found,
     * or an empty {@link Optional} if the player does not exist
     */
    public static Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return getInstance().getPlayerManager().getPlayer(uuid);
    }

    private final Gson gson;

    private final Connection connection;
    private final DataManager dataManager;

    private final ControllerPlayerManager playerManager;
    private final ControllerPartyManager partyManager;


    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances


    public Controller(Gson gson, Connection connection, DataManager dataManager) throws IOException {
        instance = this;
        this.gson = gson;
        this.connection = connection;
        this.dataManager = dataManager;
        playerManager = new ControllerPlayerManager(this, dataManager);
        partyManager = new ControllerPartyManager(this, dataManager);

        Packet.registerPackets();

        if (connection instanceof ServerSocketConnection serverSocketConnection) {
            // TODO Definitely change the type!
            serverSocketConnection.setOnClientRemove(id -> {
                instances.remove(id);
                // Kick players.
                // TODO Maybe run some other stuff when it is done? Like kicking players. Remove the instance!
            });
        }
        connection.initialise(input -> {
            Packet iPacket = input.packet();
            System.out.println("Got Packet: " + input.from());
            switch (iPacket) {
                case GameStatusUpdatePacket(long requestId, GameRecord record) -> {
                    long gameId = record.gameId();
                    if (!input.from().equals(record.instanceId())) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                        return;
                    }
                    if (!games.containsKey(gameId)) {
                        // A new game has been created, yeey!
                        games.put(gameId, new ControllerGame(record));
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                        return;
                    }
                    ControllerGame game = games.get(gameId);
                    if (!game.getInstanceId().equals(input.from())) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                        return;
                    }
                    games.get(gameId).applyRecord(record);
                    connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                }
                case GameShutdownPacket(long requestId, long gameId) -> {
                    ControllerGame game = games.get(gameId);
                    if(game == null) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from());
                        return;
                    }
                    if (!input.from().equals(game.getInstanceId())) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                        return;
                    }
                    // Remove the game
                    games.remove(gameId);
                    // Update queue
                    game.getOnline().forEach(uuid -> getPlayer(uuid).ifPresent(player -> {
                        player.setGameId(null);
                        if(player.getQueueRequest().isEmpty()) {
                            // We do not have a queue yet, requeue for a new server
                            // We NEED one, so do not allow none
                            player.queue(new QueueRequest(QueueRequestReason.GAME_SHUTDOWN, null, QueueRequestParameters.lobbyParameters), false);
                        }
                    }));
                    connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                }
                case GameQuitPacket(long requestId, UUID uuid) -> playerManager.getPlayer(uuid).ifPresentOrElse(player -> {
                    player.getGameId().flatMap(this::getGame).ifPresentOrElse(game -> {
                        if(!input.from().equals(game.getInstanceId())) {
                            connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                            return;
                        }
                        // Okay we can quit the game
                        player.setGameId(null);
                        if(player.getQueueRequest().isEmpty()) {
                            // We do not have a queue yet, requeue for a new server
                            // We NEED one, so do not allow none
                            player.queue(new QueueRequest(QueueRequestReason.GAME_QUIT, null, QueueRequestParameters.lobbyParameters), false);
                        }
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                    }, () -> connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from()));
                }, () -> connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_ID), input.from()));
                case InstanceStatusUpdatePacket(InstanceRecord record) -> {
                    if (!input.from().equals(record.id())) {
                        // TODO Report?
                        return;
                    }
                    if (!instances.containsKey(record.id())) {
                        instances.put(record.id(), new ControllerLaneInstance(record));
                        return;
                    }
                    instances.get(record.id()).applyRecord(record);
                }

                case PartyPacket.Retrieve.Request packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(
                                party -> connection.sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.OK, party.convertRecord()), input.from()),
                                () -> connection.sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.SetInvitationsOnly packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party -> {
                            party.setInvitationsOnly(packet.invitationsOnly());
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                        }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Invitation.Has packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.hasInvitation(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Invitation.Add packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.addInvitation(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Invitation.Accept packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.acceptInvitation(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Invitation.Deny packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.denyInvitation(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.JoinPlayer packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.joinPlayer(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.RemovePlayer packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.removePlayer(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Disband packet -> getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.disband()), input.from()),
                        () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.SetOwner packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.setOwner(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Warp packet -> getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.warpParty()), input.from()),
                        () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));

                case QueueRequestPacket packet -> {
                    if (packet.parameters() == null) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), "requestParameters must not be null"), input.from());
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
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), response), input.from());
                    }), () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
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
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                    Controller.getInstance().handleControllerEvent(new QueueFinishedEvent(player, queue, input.from(), null));
                                } else if (packet.gameId() != null && state.getProperties().containsKey(LaneStateProperty.GAME_ID) && state.getProperties().get(LaneStateProperty.GAME_ID).getValue().equals(packet.gameId()) && state.getName().equals(LanePlayerState.GAME_TRANSFER)) {
                                    // We joined a game.
                                    ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.GAME_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.GAME_ID, packet.gameId()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                    player.setState(newState);
                                    player.setQueueRequest(null);
                                    player.setGameId(packet.gameId());
                                    player.setInstanceId(input.from());
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                    Controller.getInstance().handleControllerEvent(new QueueFinishedEvent(player, queue, input.from(), packet.gameId()));
                                } else {
                                    // We cannot accept this queue finalization.
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                                }
                                return;
                            }
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                        }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from()));
                    }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
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
                        connection.sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    } else {
                        // TODO Do reservation, we do not want doubles!
                        connection.sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.OK, newId), input.from());
                    }
                }
                case DataObjectReadPacket packet -> {
                    if (!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new DataObjectResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    }
                    dataManager.readDataObject(packet.permissionKey(), packet.id()).whenComplete((object, ex) -> {
                        if (ex != null) {
                            // TODO Add more exceptions. To write and remove as well!
                            String result = switch (ex) {
                                case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                                case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                                default -> ResponsePacket.UNKNOWN;
                            };
                            connection.sendPacket(new DataObjectResultPacket(packet.getRequestId(), result), input.from());
                        } else {
                            connection.sendPacket(new DataObjectResultPacket(packet.getRequestId(), ResponsePacket.OK, object.orElse(null)), input.from());
                        }
                    });
                }
                case DataObjectWritePacket packet -> {
                    if (!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
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
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                        } else {
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                        }
                    });
                }
                case DataObjectRemovePacket packet -> {
                    if (!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
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
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                        } else {
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
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
                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), result), input.from());
                        } else {
                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, object), input.from());
                        }
                    });
                }
                case DataObjectCopyPacket packet -> {
                    if (!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
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
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), result), input.from());
                        } else {
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                        }
                    });
                }

                case RequestInformationPacket.Player packet ->
                        connection.sendPacket(new RequestInformationPacket.PlayerResponse(packet.getRequestId(), ResponsePacket.OK, getPlayer(packet.uuid()).map(ControllerPlayer::convertRecord).orElse(null)), input.from());
                case RequestInformationPacket.Players packet -> {
                    ArrayList<PlayerRecord> data = new ArrayList<>();
                    for (ControllerPlayer value : getPlayerManager().getPlayers()) {
                        // TODO Concurrent?
                        data.add(value.convertRecord());
                    }
                    connection.sendPacket(new RequestInformationPacket.PlayersResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
                }
                case RequestInformationPacket.Game packet ->
                        connection.sendPacket(new RequestInformationPacket.GameResponse(packet.getRequestId(), ResponsePacket.OK, getGame(packet.gameId()).map(ControllerGame::convertRecord).orElse(null)), input.from());
                case RequestInformationPacket.Games packet -> {
                    ArrayList<GameRecord> data = new ArrayList<>();
                    for (ControllerGame value : games.values()) {
                        // TODO Concurrent?
                        data.add(value.convertRecord());
                    }
                    connection.sendPacket(new RequestInformationPacket.GamesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
                }
                case RequestInformationPacket.Instance packet -> {
                    connection.sendPacket(new RequestInformationPacket.InstanceResponse(packet.getRequestId(), ResponsePacket.OK, getInstance(packet.id()).map(ControllerLaneInstance::convertRecord).orElse(null)), input.from());
                }
                case RequestInformationPacket.Instances packet -> {
                    ArrayList<InstanceRecord> data = new ArrayList<>();
                    for (ControllerLaneInstance value : instances.values()) {
                        // TODO Concurrent?
                        data.add(value.convertRecord());
                    }
                    connection.sendPacket(new RequestInformationPacket.InstancesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
                }
                case RequestInformationPacket.PlayerUsername packet ->
                        getPlayerManager().getPlayerUsername(packet.uuid()).whenComplete((username, ex) -> {
                            if (ex != null) {
                                // TODO Additional instnceof? As read?
                                if (ex instanceof IllegalArgumentException) {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                    return;
                                }
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                return;
                            }
                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, username.orElse(null)), input.from());
                        });
                case RequestInformationPacket.PlayerUuid packet ->
                        getPlayerManager().getPlayerUuid(packet.username()).whenComplete((uuid, ex) -> {
                            if (ex != null) {
                                // TODO Additional instnceof? As read?
                                if (ex instanceof IllegalArgumentException) {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                    return;
                                }
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                return;
                            }
                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, uuid.orElse(null)), input.from());
                        });
                case SendMessagePacket packet -> {
                    sendMessage(packet.player(), packet.message());
                }

                case SavedLocalePacket.Get packet -> {
                    getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(profile -> getPlayerManager().getSavedLocale(profile))
                            .whenComplete((locale, ex) -> {
                                if (ex != null) {
                                    // TODO Additional instnceof? As read?
                                    if (ex instanceof IllegalArgumentException) {
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                if (locale.isPresent()) {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, locale.get().toLanguageTag()), input.from());
                                } else {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, null), input.from());
                                }
                            });
                }
                case SavedLocalePacket.Set packet -> {
                    getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(profile -> getPlayerManager().setSavedLocale(profile, Locale.forLanguageTag(packet.locale())).thenApply(data -> profile))
                            .whenComplete((profile, ex) -> {
                                if (ex != null) {
                                    // TODO Additional instnceof? As write?
                                    if (ex instanceof IllegalArgumentException) {
                                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                setEffectiveLocale(profile.getFirstSuperProfile(), Locale.forLanguageTag(packet.locale())); // TODO On evnet
                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                            });
                }
                case ProfilePacket profilePacket -> {
                    switch (profilePacket) {
                        case ProfilePacket.GetProfileData packet ->
                                getProfileData(packet.uuid()).whenComplete((opt, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, opt.map(ControllerProfileData::convertRecord).orElse(null)), input.from());
                                });
                        case ProfilePacket.CreateNew packet ->
                                createNewProfile(packet.type()).whenComplete((profile, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                                });
                        case ProfilePacket.CreateSubProfile packet -> getProfileData(packet.current()).thenApply(opt -> opt.orElse(null))
                                .thenCompose(current -> createSubProfile(current, packet.type(), packet.name(), packet.active()))
                                .whenComplete((profile, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                                });
                        case ProfilePacket.AddSubProfile packet -> getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getProfileData(packet.subProfile())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(subProfile -> addSubProfile(current, subProfile, packet.name(), packet.active())))
                                .whenComplete((status, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                                });
                        case ProfilePacket.RemoveSubProfile packet -> getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getProfileData(packet.subProfile())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(subProfile -> removeSubProfile(current, subProfile, packet.name())))
                                .whenComplete((status, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                                });
                        case ProfilePacket.ResetDelete packet -> getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current -> resetDeleteProfile(current, packet.delete()))
                                .whenComplete((status, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As write?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        if(ex instanceof IllegalStateException){
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                });
                        case ProfilePacket.Copy packet -> getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getProfileData(packet.from())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(from -> copyProfile(current, from)))
                                .whenComplete((status, ex) -> {
                                    if (ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if (ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                });
                        case ProfilePacket.SetNetworkProfile packet -> {
                            ControllerPlayer player = getPlayer(packet.player()).orElse(null);
                            getProfileData(packet.profile())
                                    .thenApply(opt -> opt.orElse(null))
                                    .thenCompose(profile -> setNetworkProfile(player, profile))
                                    .whenComplete((status, ex) -> {
                                        if (ex != null) {
                                            // TODO Additional instnceof? As write?
                                            if (ex instanceof IllegalArgumentException) {
                                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                                return;
                                            }
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                    });
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + profilePacket);
                    }
                }
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
                                    connection.sendPacket(new VoidResultPacket(requestId, response), input.from());
                                }), () -> connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INVALID_PLAYER), input.from()));
                        default -> throw new IllegalStateException("Unexpected value: " + setInformation);
                    }
                }
                case ResponsePacket<?> response -> {
                    if (!connection.retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
                        // TODO Well, log about packet that is not wanted.
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + iPacket);
            }
        });
    }

    public void shutdown() {
        connection.close();
        dataManager.shutdown();
        // TODO Probably more
    }

    public Connection getConnection() {
        return connection;
    }

    private DataManager getDataManager() {
        return dataManager;
    }

    public ControllerPlayerManager getPlayerManager() {
        return playerManager;
    }

    public ControllerPartyManager getPartyManager() {
        return partyManager;
    }

    public Optional<ControllerLaneInstance> getInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    } // TODO Really public?

    public Collection<ControllerLaneInstance> getInstances() { // TODO Really public?
        return instances.values();
    }

    /**
     * Retrieves the data object at the given id with the given permission key.
     * When no data object exists at the given id, the optional is empty.
     * Otherwise, the data object is populated with the given data.
     * When the permission key does not grant reading, no information besides the id is filled in.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the individual permission key to use while reading
     * @param id            the id of the data object to request
     * @return a completable future with an optional with the data object
     */
    public CompletableFuture<Optional<DataObject>> readDataObject(PermissionKey permissionKey, DataObjectId id) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.readDataObject(permissionKey, id);
    }

    /**
     * Writes the data object at the given id with the given permission key.
     * When no data object exists at the given id, it is created.
     * Otherwise, the data object is updated.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while writing
     * @param object        the data object to update it with
     * @return a completable future with the void type to signify success: it has been written
     */
    public CompletableFuture<Void> writeDataObject(PermissionKey permissionKey, DataObject object) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.writeDataObject(permissionKey, object);
        // TODO We should check we do not overwrite info we reserve in defaultData.md
    }

    /**
     * Removes the data object at the given id with the given permission key.
     * When the permission key does not grant removing, it is not removed, and a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while removing
     * @param id            the id of the data object to remove
     * @return a completable future with the void type to signify success: it was removed or did not exist
     */
    public CompletableFuture<Void> removeDataObject(PermissionKey permissionKey, DataObjectId id) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.removeDataObject(permissionKey, id);
    }

    /**
     * Updates the data object at the given id with the given permission key.
     * First the data object is read from the given id, then is accepted in the consumer.
     * The function can modify the values within the given data object.
     * After the consumer has been run, the updated data object is written back.
     * It is only written if the updater has returned true.
     * When the permission key does not grant writing, a {@link PermissionFailedException} is thrown in the {@link CompletableFuture}.
     * If the permission key is not an individual key, the completable future is thrown with an {@link IllegalArgumentException}.
     *
     * @param permissionKey the permission key to use while reading and writing
     * @param id            the id of the data object to update
     * @param updater       the updater consumer that handles the update
     * @return a completable future with the status as boolean: true if updated successfully or when the updater had returned false,
     * false when the data object did not exist.
     */
    public CompletableFuture<Boolean> updateDataObject(PermissionKey permissionKey, DataObjectId id, Function<DataObject, Boolean> updater) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.updateDataObject(permissionKey, id, updater);
    }

    /**
     * Retrieves a list of data object IDs whose key has the same prefix from the provided ID (case sensitive).
     * Example for the input with id = "myPrefix" with relationalId = ("players", "Laurenshup"), it will return:
     * <ul>
     *     <li>players.Laurenshup.myPrefix.value1</li>
     *     <li>players.Laurenshup.myPrefix.value2.subKey</li>
     *     <li>players.Laurenshup.myPrefixSuffix</li>
     * </ul>
     * @param prefix the prefix ID. This cannot be null, its values can be null.
     * @return a {@link CompletableFuture} with the array of IDs with matching prefix
     */
    public CompletableFuture<ArrayList<DataObjectId>> listDataObjectIds(DataObjectId prefix) {
        return dataManager.listDataObjectIds(prefix);
    }

    /**
     * Copies a data object from one place to another.
     * This completely copies the data object, but replaces the ID.
     * @param permissionKey the permission key to use while reading and writing
     * @param sourceId the source data object ID
     * @param targetId the target data object ID
     * @return a {@link CompletableFuture} with the void type to signify success: it has been copied
     */
    CompletableFuture<Void> copyDataObject(PermissionKey permissionKey, DataObjectId sourceId, DataObjectId targetId) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.copyDataObject(permissionKey, sourceId, targetId);
    }

    /**
     * Retrieves the profile data of the profile identified by the given UUID.
     *
     * @param uuid the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will be the profile data if present
     */
    public CompletableFuture<Optional<ControllerProfileData>> getProfileData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Profiles(uuid), "data"))
                .thenApply(optDataObj -> optDataObj.flatMap(dataObj -> dataObj.getValueAsJson(gson, ControllerProfileData.class)));
    }

    /**
     * Creates a new profile given the profile type.
     * This stores the profile information at a new profile UUID.
     *
     * @param type the profile type
     * @return a {@link CompletableFuture} with a {@link UUID}, which is the UUID of the new profile
     */
    public CompletableFuture<ControllerProfileData> createNewProfile(@NotNull ProfileType type) {
        Objects.requireNonNull(type, "type cannot be null");
        UUID uuid = UUID.randomUUID();
        return getProfileData(uuid).thenCompose(optProfile -> {
            if (optProfile.isPresent()) return createNewProfile(type);
            // Our UUID is unique, now create
            // TODO This can still cause troubles! We should do reservation: write into reservation array somewhere, even before getProfileData!
            ControllerProfileData newData = new ControllerProfileData(uuid, type);
            DataObject dataObject = new DataObject(newData.getDataObjectId(), PermissionKey.CONTROLLER, gson, newData);
            return dataManager.writeDataObject(PermissionKey.CONTROLLER, dataObject).thenApply(none -> newData);
        });
    }

    /**
     * Creates a sub profile to another "super profile", the current profile, at the given name.
     * This returns a {@link CompletableFuture} with the profile that has been made and added to the super profile.
     * Internally, first creates a new profile;
     * after which the current profile is added as super profile in the sub profile;
     * then the sub profile is added to the current profile.
     * These changes are reflected in the respective parameters' values as well.
     * The type cannot be of type {@link ProfileType#NETWORK}.
     * @param current the current profile, where to create the sub profile
     * @param type the profile type to create
     * @param name the name to add the sub profile to
     * @param active whether the sub profile is active
     * @return a {@link CompletableFuture} with the new profile data if successful
     */
    public @NotNull CompletableFuture<ControllerProfileData> createSubProfile(@NotNull ControllerProfileData current, @NotNull ProfileType type, @NotNull String name, boolean active) {
        return createNewProfile(type).thenCompose(subProfile -> addSubProfile(current, subProfile, name, active).thenCompose(status -> {
            if(!status) {
                resetDeleteProfile(subProfile, true);
                return CompletableFuture.failedFuture(new IllegalStateException("Could not add the newly created sub profile to the current profile"));
            }
            return CompletableFuture.completedFuture(subProfile);
        }));
    }

    /**
     * Adds a sub profile to another "super profile", the current profile at the given name.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been added or not.
     * Internally, first adds the current profile as super profile in the sub profile;
     * after which the sub profile is added to the current profile.
     * These changes are reflected in the respective parameters' values as well.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.
     * If the sub profile already exists at the given name and profile, it still updates the active state.
     *
     * @param current    the current profile, where to add the sub profile to
     * @param subProfile the sub profile to add
     * @param name       the name to add the sub profile to
     * @param active     whether the sub profile is active
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been added, {@code false} otherwise
     */
    public CompletableFuture<Boolean> addSubProfile(ControllerProfileData current, ControllerProfileData subProfile, String name, boolean active) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (subProfile.getType() == ProfileType.NETWORK) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot add a network profile as sub profile"));
        }
        if (subProfile.getType() == ProfileType.SUB && !subProfile.getSuperProfiles().isEmpty()) {
            // Check whether we update the inactive/active state
            if(!current.getSubProfileData(subProfile.getId()).containsKey(name)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("This sub profile already has a super profile which is not the current profile and current name"));
            }
        }

        // First we update the sub profile so that it holds the super profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, subProfile.getDataObjectId(), obj -> {
            subProfile.addSuperProfile(current.getId());
            obj.setValue(gson, subProfile);
            return true;
        }).thenCompose(status -> {
            if (!status) {
                return CompletableFuture.failedFuture(new IllegalStateException("Profile data of sub profile did not exist"));
            }
            // We know we updated the sub profile, now update the current one.
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, current.getDataObjectId(), obj -> {
                current.addSubProfile(subProfile.getId(), name, active);
                obj.setValue(gson, current);
                return true;
            });
        });
    }

    /**
     * Removes a sub profile from another "super profile", the current profile at the given name.
     * If the name is null, the sub profile is removed at all positions in the super profile.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been removed or not.
     * Internally, first removes the sub profile from the current profile;
     * after which the super profile is removed from the sub profile.
     * These changes are reflected in the respective parameters' values as well.
     *
     * @param current    the current profile, where to remove the sub profile from
     * @param subProfile the sub profile to remove
     * @param name       the name to remove the sub profile from, or null to completely remove it
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been removed, {@code false} otherwise
     */
    public CompletableFuture<Boolean> removeSubProfile(ControllerProfileData current, ControllerProfileData subProfile, String name) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");

        // First we update the super profile so that it does not hold the sub profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, current.getDataObjectId(), obj -> {
            current.removeSubProfile(subProfile.getId(), name);
            obj.setValue(gson, current);
            return true;
        }).thenCompose(status -> {
            if (!status) return CompletableFuture.completedFuture(false);
            // We know we updated the current profile, now update the sub profile.
            if(!current.getSubProfileData(subProfile.getId()).isEmpty()) {
                // We do not need to update, it is still a super profile
                return CompletableFuture.completedFuture(true);
            }
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, subProfile.getDataObjectId(), obj -> {
                subProfile.removeSuperProfile(current.getId());
                obj.setValue(gson, subProfile);
                return true;
            });
        });
    }

    /**
     * Resets or deletes the profile, see {@link ProfileData#resetProfile()} and {@link ProfileData#deleteProfile()}.
     * This removes all data objects for the specified profile, when only resetting, this leaves the profile data intact.
     * If the profile type is {@link ProfileType#NETWORK}, this can only be done when the profile has no super profile.
     *
     * @param current the profile
     * @param delete  whether to also remove the profile data info
     * @return a {@link CompletableFuture} with a void to signify success: it has been reset/deleted completely
     */
    public CompletableFuture<Void> resetDeleteProfile(ControllerProfileData current, boolean delete) {
        Objects.requireNonNull(current, "current cannot be null");
        if (delete && (!current.getSuperProfiles().isEmpty() || !current.getSubProfiles().isEmpty())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot delete profile with sub or super profiles"));
        }
        return dataManager.listDataObjectIds(new DataObjectId(RelationalId.Profiles(current.getId()), null)).thenCompose(dataObjectIds -> {
            if (!delete)
                dataObjectIds.remove(current.getDataObjectId()); // Remove the data object information if deleting
            // Create futures that remove every one of them
            CompletableFuture<?>[] futures = new CompletableFuture[dataObjectIds.size()];
            for (int i = 0; i < dataObjectIds.size(); i++) {
                futures[i] = dataManager.removeDataObject(PermissionKey.CONTROLLER, dataObjectIds.get(i));
            }
            // Combine the futures into one that only accepts if all of them do
            return CompletableFuture.allOf(futures);
        }).exceptionally(val -> {
            val.printStackTrace();
            return null;
        });
    }

    /**
     * Copies one profile to another, see {@link ProfileData#copyProfile(ProfileData)}, this does not copy the profile data information object.
     *
     * @param current the profile to copy to
     * @param from    the profile to copy from
     * @return a {@link CompletableFuture} with a void to signify success: it has been copied completely
     */
    public CompletableFuture<Void> copyProfile(ControllerProfileData current, ProfileData from) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(from, "from cannot be null");
        return dataManager.listDataObjectIds(new DataObjectId(RelationalId.Profiles(from.getId()), null)).thenCompose(dataObjectIds -> {
            dataObjectIds.remove(from.getDataObjectId()); // Remove the data object information
            // Create futures that copy every one of them
            CompletableFuture<?>[] futures = new CompletableFuture[dataObjectIds.size()];
            for (int i = 0; i < dataObjectIds.size(); i++) {
                DataObjectId fromDataObject = dataObjectIds.get(i);
                DataObjectId toDataObject = new DataObjectId(RelationalId.Profiles(current.getId()), fromDataObject.id());
                System.out.println("Copying " + fromDataObject + " to " + toDataObject);
                futures[i] = dataManager.copyDataObject(PermissionKey.CONTROLLER, fromDataObject, toDataObject);
            }
            // Combine the futures into one that only accepts if all of them do
            return CompletableFuture.allOf(futures);
        });
    }

    /**
     * Sets the network profile of the given player to the provided profile.
     * The profile must be of type {@link ProfileType#NETWORK}.
     *
     * @param player  the player
     * @param profile the profile
     * @return a {@link CompletableFuture} to signify success: the profile has been set
     */
    public CompletableFuture<Void> setNetworkProfile(ControllerPlayer player, ControllerProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check if already set
        if (player.getNetworkProfileUuid().equals(profile.getId())) return CompletableFuture.completedFuture(null);
        // Check whether the profile can be set
        if (profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        // Retrieve old profile
        return player.getNetworkProfile().thenCompose(oldProfile -> {
            // Update profiles, first we update the sub profile so that it holds the super profile.
            return dataManager.updateDataObject(PermissionKey.CONTROLLER, profile.getDataObjectId(), obj -> {
                // TODO What if we could not continue, this would be set. And the sub profile has multiple super profiles!!!!
                //  Check this at all locations.
                profile.addSuperProfile(player.getUuid());
                obj.setValue(gson, profile);
                return true;
            }).thenCompose(status -> {
                if (status) {
                    // We added the super profile. Now replace the network profile
                    return DefaultDataObjects.setPlayersNetworkProfile(dataManager, player.getUuid(), profile.getId())
                            .thenCompose(data -> {
                                // We can remove the super profile from the original one
                                return dataManager.updateDataObject(PermissionKey.CONTROLLER, oldProfile.getDataObjectId(), obj -> {
                                    oldProfile.removeSuperProfile(player.getUuid());
                                    obj.setValue(gson, oldProfile);
                                    return true;
                                }).thenAccept(status2 -> {
                                    // Super profile has been removed from the original one
                                    // Super profile from new one is the player, player has the new one. Just object object
                                    player.setNetworkProfileUuid(profile.getId());
                                });
                            });
                }
                return CompletableFuture.failedFuture(new UnsuccessfulResultException("Could not set super profile to new profile"));
            });
        });
    }

    /**
     * Sets a network profile, as if the player did not have one currently.
     * This is an internal function for the Controller when a player has joined the network newly.
     *
     * @param player  the player's UUID
     * @param profile the profile
     * @return a {@link CompletableFuture} with a void to signify succes: the new profile has been set
     */
    CompletableFuture<Void> setNewNetworkProfile(UUID player, ControllerProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check whether the profile can be set
        if (profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        // Update profiles, first we update the sub profile so that it holds the super profile.
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, profile.getDataObjectId(), obj -> {
            // TODO What if we could not continue, this would be set. And the sub profile has multiple super profiles!!!!
            //  Check this at all locations.
            profile.addSuperProfile(player);
            obj.setValue(gson, profile);
            return true;
        }).thenCompose(status -> {
            if (status) {
                // We added the super profile. Now replace the network profile
                return DefaultDataObjects.setPlayersNetworkProfile(dataManager, player, profile.getId());
            }
            return CompletableFuture.failedFuture(new UnsuccessfulResultException("Could not set super profile to new profile"));
        });
    }

    public Collection<ControllerGame> getGames() {
        return games.values();
    } // TODO Redo

    public Optional<ControllerGame> getGame(long id) {
        return Optional.ofNullable(games.get(id));
    } // TODO Redo

    /**
     * Requests the instance to shut down the game.
     *
     * @param game the game to shut down
     * @return a {@link CompletableFuture} with a void to signify success: it has been shut down
     */
    public CompletableFuture<Void> shutdownGame(ControllerGame game) {
        return connection.<Void>sendRequestPacket(id -> new GameShutdownRequestPacket(id, game.getGameId()), game.getInstanceId()).getResult();
    }

    /**
     * Method that will switch the server of the given player to the given server.
     * This should not do anything when the player is already connected to the given server.
     *
     * @param uuid        the player's uuid
     * @param destination the server's id
     * @return the completable future with the result
     */
    public abstract CompletableFuture<Void> joinServer(UUID uuid, String destination);

    /**
     * Lets the implemented controller handle the Lane Controller event.
     * Some events have results tied to them, which are to be expected to return in the CompletableFuture.
     * Some events might not have the possibility to wait for the result asynchronously, so that the CompletableFuture is waited for.
     *
     * @param event the event to handle
     * @param <E>   the Lane controller event type
     * @return the CompletableFuture with the modified event
     */
    public abstract <E extends ControllerEvent> CompletableFuture<E> handleControllerEvent(E event);


    // TODO These implementation dependent functions could technically also use ControllerPlayer instead of UUID?

    /**
     * Send a message to the player with the given UUID.
     *
     * @param player  the player's UUID
     * @param message the message to send
     */
    public abstract void sendMessage(UUID player, Component message);

    /**
     * Disconnect the player with the given message from the network.
     *
     * @param player  The player's UUID
     * @param message The message to show when disconnecting
     */
    public abstract void disconnectPlayer(UUID player, Component message);

    /**
     * Sets the effective locale for the given player.
     *
     * @param player the UUID of the player whose effective locale is being set
     * @param locale the locale to set as the effective locale for the player
     */
    public abstract void setEffectiveLocale(UUID player, Locale locale);

    /**
     * Gets the effective locale for the given player.
     *
     * @param player the UUID of the player whose effective locale is being retrieved
     * @return the effective locale, or null if the player is not online
     */
    public abstract Locale getEffectiveLocale(UUID player);

}