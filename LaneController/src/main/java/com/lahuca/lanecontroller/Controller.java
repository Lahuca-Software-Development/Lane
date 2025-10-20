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
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.queue.QueueRequestReason;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lanecontroller.events.ControllerEvent;
import com.lahuca.lanecontroller.events.QueueFinishedEvent;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    private final ControllerDataManager controllerDataManager;
    private final ControllerPlayerManager playerManager;
    private final ControllerPartyManager partyManager;
    private final ControllerFriendshipManager friendshipManager;

    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances


    public Controller(Gson gson, Connection connection, DataManager dataManager) throws IOException {
        instance = this;
        this.gson = gson;
        this.connection = connection;
        this.dataManager = dataManager;
        controllerDataManager = new ControllerDataManager(this, dataManager, gson);
        playerManager = new ControllerPlayerManager(this, dataManager);
        partyManager = new ControllerPartyManager(this, dataManager);
        friendshipManager = new ControllerFriendshipManager(this, dataManager, gson);

        Packet.registerPackets();

        if(connection instanceof ServerSocketConnection serverSocketConnection) {
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
            switch(iPacket) {
                case GameStatusUpdatePacket(long requestId, GameRecord record) -> {
                    long gameId = record.gameId();
                    if(!input.from().equals(record.instanceId())) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                        return;
                    }
                    if(!games.containsKey(gameId)) {
                        // A new game has been created, yeey!
                        games.put(gameId, new ControllerGame(record));
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                        return;
                    }
                    ControllerGame game = games.get(gameId);
                    if(!game.getInstanceId().equals(input.from())) {
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
                    if(!input.from().equals(game.getInstanceId())) {
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
                case GameQuitPacket(long requestId, UUID uuid) ->
                        playerManager.getPlayer(uuid).ifPresentOrElse(player -> {
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
                    if(!input.from().equals(record.id())) {
                        // TODO Report?
                        return;
                    }
                    if(!instances.containsKey(record.id())) {
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
                    if(packet.parameters() == null) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), "requestParameters must not be null"), input.from());
                        return;
                    }
                    getPlayer(packet.player()).ifPresentOrElse(player -> player.queue(new QueueRequest(QueueRequestReason.PLUGIN_INSTANCE, packet.parameters()), true).whenComplete((result, exception) -> {
                        String response = ResponsePacket.OK;
                        if(exception != null) {
                            if(exception instanceof UnsuccessfulResultException ex) {
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
                            if(state != null && state.getProperties() != null && state.getProperties().containsKey(LaneStateProperty.INSTANCE_ID)) {
                                // Check if we either joined the correct instance or game.
                                if(state.getName().equals(LanePlayerState.INSTANCE_TRANSFER) && state.getProperties().get(LaneStateProperty.INSTANCE_ID).getValue().equals(input.from())) {
                                    // We joined an instance.
                                    ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.INSTANCE_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                    player.setState(newState);
                                    player.setQueueRequest(null);
                                    player.setGameId(null);
                                    player.setInstanceId(input.from());
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                    Controller.getInstance().handleControllerEvent(new QueueFinishedEvent(player, queue, input.from(), null));
                                } else if(packet.gameId() != null && state.getProperties().containsKey(LaneStateProperty.GAME_ID) && state.getProperties().get(LaneStateProperty.GAME_ID).getValue().equals(packet.gameId()) && state.getName().equals(LanePlayerState.GAME_TRANSFER)) {
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
                    switch(packet.type()) {
                        case GAME -> {
                            do {
                                newId = System.currentTimeMillis();
                            } while(games.containsKey(newId));
                        }
                        default -> newId = null;
                    }
                    if(newId == null) {
                        connection.sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    } else {
                        // TODO Do reservation, we do not want doubles!
                        connection.sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.OK, newId), input.from());
                    }
                }
                case DataObjectReadPacket packet -> {
                    if(!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new DataObjectResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    }
                    dataManager.readDataObject(packet.permissionKey(), packet.id()).whenComplete((object, ex) -> {
                        if(ex != null) {
                            // TODO Add more exceptions. To write and remove as well!
                            String result = switch(ex) {
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
                    if(!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    }
                    dataManager.writeDataObject(packet.permissionKey(), packet.object()).whenComplete((bool, ex) -> {
                        if(ex != null) {
                            String result = switch(ex) {
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
                    if(!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    }
                    dataManager.removeDataObject(packet.permissionKey(), packet.id()).whenComplete((bool, ex) -> {
                        if(ex != null) {
                            String result = switch(ex) {
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
                        if(ex != null) {
                            // TODO Add more exceptions. To write and remove as well!
                            String result = switch(ex) {
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

                case DataObjectsListPacket packet -> {
                    dataManager.listDataObjects(packet.prefix(), packet.permissionKey(), packet.version())
                            .whenComplete((object, ex) -> {
                                if(ex != null) {
                                    // TODO Add more exceptions. To write and remove as well!
                                    String result = switch(ex) {
                                        case PermissionFailedException ignored -> ResponsePacket.INSUFFICIENT_RIGHTS;
                                        case IllegalArgumentException ignored -> ResponsePacket.ILLEGAL_ARGUMENT;
                                        default -> ResponsePacket.UNKNOWN;
                                    };
                                    connection.sendPacket(new DataObjectsResultPacket(packet.getRequestId(), result), input.from());
                                } else {
                                    connection.sendPacket(new DataObjectsResultPacket(packet.getRequestId(), ResponsePacket.OK, object), input.from());
                                }
                            });
                }

                case DataObjectCopyPacket packet -> {
                    if(!packet.permissionKey().isIndividual()) {
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                    }
                    dataManager.copyDataObject(packet.permissionKey(), packet.sourceId(), packet.targetId()).whenComplete((bool, ex) -> {
                        if(ex != null) {
                            String result = switch(ex) {
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
                    for(ControllerPlayer value : getPlayerManager().getPlayers()) {
                        // TODO Concurrent?
                        data.add(value.convertRecord());
                    }
                    connection.sendPacket(new RequestInformationPacket.PlayersResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
                }
                case RequestInformationPacket.Game packet ->
                        connection.sendPacket(new RequestInformationPacket.GameResponse(packet.getRequestId(), ResponsePacket.OK, getGame(packet.gameId()).map(ControllerGame::convertRecord).orElse(null)), input.from());
                case RequestInformationPacket.Games packet -> {
                    ArrayList<GameRecord> data = new ArrayList<>();
                    for(ControllerGame value : games.values()) {
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
                    for(ControllerLaneInstance value : instances.values()) {
                        // TODO Concurrent?
                        data.add(value.convertRecord());
                    }
                    connection.sendPacket(new RequestInformationPacket.InstancesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
                }
                case RequestInformationPacket.PlayerUsername packet ->
                        getPlayerManager().getPlayerUsername(packet.uuid()).whenComplete((username, ex) -> {
                            if(ex != null) {
                                // TODO Additional instnceof? As read?
                                if(ex instanceof IllegalArgumentException) {
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
                            if(ex != null) {
                                // TODO Additional instnceof? As read?
                                if(ex instanceof IllegalArgumentException) {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                    return;
                                }
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                return;
                            }
                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, uuid.orElse(null)), input.from());
                        });
                case RequestInformationPacket.PlayerNetworkProfile packet ->
                        getPlayerManager().getPlayerNetworkProfile(packet.uuid())
                                .whenComplete((data, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, data.map(ProfileData::convertRecord).orElse(null)), input.from());
                                });
                case SendMessagePacket packet -> {
                    sendMessage(packet.player(), packet.message());
                }

                case SavedLocalePacket.Get packet -> {
                    getDataManager().getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(profile -> getPlayerManager().getSavedLocale(profile))
                            .whenComplete((locale, ex) -> {
                                if(ex != null) {
                                    // TODO Additional instnceof? As read?
                                    if(ex instanceof IllegalArgumentException) {
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                    return;
                                }
                                if(locale.isPresent()) {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, locale.get().toLanguageTag()), input.from());
                                } else {
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, null), input.from());
                                }
                            });
                }
                case SavedLocalePacket.Set packet -> {
                    getDataManager().getProfileData(packet.networkProfile()).thenApply(opt -> opt.orElse(null))
                            .thenCompose(profile -> getPlayerManager().setSavedLocale(profile, Locale.forLanguageTag(packet.locale())).thenApply(data -> profile))
                            .whenComplete((profile, ex) -> {
                                if(ex != null) {
                                    // TODO Additional instnceof? As write?
                                    if(ex instanceof IllegalArgumentException) {
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
                    switch(profilePacket) {
                        case ProfilePacket.GetProfileData packet ->
                                getDataManager().getProfileData(packet.uuid()).whenComplete((opt, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, opt.map(ControllerProfileData::convertRecord).orElse(null)), input.from());
                                });
                        case ProfilePacket.CreateNew packet ->
                                getDataManager().createNewProfile(packet.type()).whenComplete((profile, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                                });
                        case ProfilePacket.CreateSubProfile packet ->
                                getDataManager().getProfileData(packet.current()).thenApply(opt -> opt.orElse(null))
                                        .thenCompose(current -> getDataManager().createSubProfile(current, packet.type(), packet.name(), packet.active()))
                                        .whenComplete((profile, ex) -> {
                                            if(ex != null) {
                                                // TODO Additional instnceof? As read?
                                                if(ex instanceof IllegalArgumentException) {
                                                    connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                                    return;
                                                }
                                                connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                                return;
                                            }
                                            connection.sendPacket(new ProfileRecordResultPacket(packet.getRequestId(), ResponsePacket.OK, profile.convertRecord()), input.from());
                                        });
                        case ProfilePacket.AddSubProfile packet -> getDataManager().getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getDataManager().getProfileData(packet.subProfile())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(subProfile -> getDataManager().addSubProfile(current, subProfile, packet.name(), packet.active())))
                                .whenComplete((status, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                                });
                        case ProfilePacket.RemoveSubProfile packet -> getDataManager().getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getDataManager().getProfileData(packet.subProfile())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(subProfile -> getDataManager().removeSubProfile(current, subProfile, packet.name())))
                                .whenComplete((status, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, status), input.from());
                                });
                        case ProfilePacket.ResetDelete packet -> getDataManager().getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current -> getDataManager().resetDeleteProfile(current, packet.delete()))
                                .whenComplete((status, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As write?
                                        if(ex instanceof IllegalArgumentException) {
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                            return;
                                        }
                                        if(ex instanceof IllegalStateException) {
                                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                                            return;
                                        }
                                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                                        return;
                                    }
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                });
                        case ProfilePacket.Copy packet -> getDataManager().getProfileData(packet.current())
                                .thenApply(opt -> opt.orElse(null))
                                .thenCompose(current ->
                                        getDataManager().getProfileData(packet.from())
                                                .thenApply(opt2 -> opt2.orElse(null))
                                                .thenCompose(from -> getDataManager().copyProfile(current, from)))
                                .whenComplete((status, ex) -> {
                                    if(ex != null) {
                                        // TODO Additional instnceof? As read?
                                        if(ex instanceof IllegalArgumentException) {
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
                            getDataManager().getProfileData(packet.profile())
                                    .thenApply(opt -> opt.orElse(null))
                                    .thenCompose(profile -> getDataManager().setNetworkProfile(player, profile))
                                    .whenComplete((status, ex) -> {
                                        if(ex != null) {
                                            // TODO Additional instnceof? As write?
                                            if(ex instanceof IllegalArgumentException) {
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
                    switch(setInformation) {
                        case SetInformationPacket.PlayerSetNickname(long requestId, UUID uuid, String nickname) ->
                                getPlayer(uuid).ifPresentOrElse(player -> player.setNickname(nickname).whenComplete((result, exception) -> {
                                    String response = ResponsePacket.OK;
                                    if(exception != null) {
                                        if(exception instanceof UnsuccessfulResultException ex) {
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
                    if(!connection.retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
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

    private DataManager dataManager() {
        return dataManager;
    }

    public ControllerDataManager getDataManager() {
        return controllerDataManager;
    }

    public ControllerPlayerManager getPlayerManager() {
        return playerManager;
    }

    public ControllerPartyManager getPartyManager() {
        return partyManager;
    }

    public ControllerFriendshipManager getFriendshipManager() {
        return friendshipManager;
    }

    public Optional<ControllerLaneInstance> getInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    } // TODO Really public?

    public Collection<ControllerLaneInstance> getInstances() { // TODO Really public?
        return instances.values();
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