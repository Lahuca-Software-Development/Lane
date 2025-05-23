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

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.data.DataObjectReadPacket;
import com.lahuca.lane.connection.packet.data.DataObjectRemovePacket;
import com.lahuca.lane.connection.packet.data.DataObjectWritePacket;
import com.lahuca.lane.connection.packet.data.SavedLocalePacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.connection.request.result.DataObjectResultPacket;
import com.lahuca.lane.connection.request.result.LongResultPacket;
import com.lahuca.lane.connection.request.result.SimpleResultPacket;
import com.lahuca.lane.connection.request.result.VoidResultPacket;
import com.lahuca.lane.connection.socket.server.ServerSocketConnection;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.queue.*;
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lanecontroller.events.QueueStageEvent;
import com.lahuca.lanecontroller.events.QueueStageEventResult;
import com.lahuca.lanecontroller.managers.ControllerPlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

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

    private final Connection connection;
    private final DataManager dataManager;

    private final ControllerPlayerManager playerManager;

    private final HashMap<Long, ControllerParty> parties = new HashMap<>();
    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances


    public Controller(Connection connection, DataManager dataManager) throws IOException {
        instance = this;
        this.connection = connection;
        this.dataManager = dataManager;
        playerManager = new ControllerPlayerManager(this, dataManager);

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
            if (iPacket instanceof GameStatusUpdatePacket packet) {
                if (!games.containsKey(packet.gameId())) {
                    // A new game has been created, yeey!
                    ControllerGameState initialState = new ControllerGameState(packet.state());
                    games.put(packet.gameId(),
                            new ControllerGame(packet.gameId(), input.from(),
                                    packet.name(), initialState));
                    connection.sendPacket(new VoidResultPacket(packet.requestId(), ResponsePacket.OK), input.from());
                    return;
                }
                ControllerGame game = games.get(packet.gameId());
                if (!game.getInstanceId().equals(input.from())) {
                    connection.sendPacket(new VoidResultPacket(packet.requestId(), ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                    return;
                }
                games.get(packet.gameId()).update(packet.name(), packet.state());
                connection.sendPacket(new VoidResultPacket(packet.requestId(), ResponsePacket.OK), input.from());
            } else if (iPacket instanceof InstanceStatusUpdatePacket packet) {
                createGetInstance(input.from()).applyRecord(packet.record());
            } else if (iPacket instanceof PartyPacket.Retrieve.Request packet) {
                getParty(packet.partyId()).ifPresentOrElse(party -> connection.sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.OK, party.convertToRecord()), input.from()),
                        () -> connection.sendPacket(new PartyPacket.Retrieve.Response(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            } else if (iPacket instanceof PartyPacket.Player.Add packet) {
                getParty(packet.partyId()).ifPresentOrElse(party -> getPlayer(packet.player()).ifPresentOrElse(party::addPlayer,
                                () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                        () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            } else if (iPacket instanceof PartyPacket.Player.Remove packet) {
                getParty(packet.partyId()).ifPresentOrElse(party -> getPlayer(packet.player()).ifPresentOrElse(party::removePlayer,
                                () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                        () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            } else if (iPacket instanceof PartyPacket.Disband.Request packet) {
                getParty(packet.partyId()).ifPresentOrElse(this::disbandParty, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
            } else if (iPacket instanceof QueueRequestPacket packet) {
                getPlayer(packet.player()).ifPresentOrElse(player -> {
                    queue(player, new QueueRequest(QueueRequestReason.PLUGIN_INSTANCE, packet.parameters())).whenComplete((result, exception) -> {
                        String response;
                        if (exception != null) {
                            response = ResponsePacket.UNKNOWN;
                        } else {
                            response = result.result();
                        }
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), response), input.from());
                    });
                }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
            } else if (iPacket instanceof QueueFinishedPacket packet) {
                System.out.println("Retrieved finish queue");
                getPlayer(packet.player()).ifPresentOrElse(player -> {
                    System.out.println("Retrieved finish queue 0");
                    // Player should have finished its queue, check whether it is allowed.
                    player.getQueueRequest().ifPresentOrElse(queue -> {
                        System.out.println("Retrieved finish queue 1");
                        // There is a queue, check if the state of the player was to transfer to the retrieved instance/game.
                        ControllerPlayerState state = player.getState();
                        if (state != null && state.getProperties() != null && state.getProperties().containsKey(LaneStateProperty.INSTANCE_ID)) {
                            System.out.println("Retrieved finish queue 2");
                            // Check if we either joined the correct instance or game.
                            if (state.getName().equals(LanePlayerState.INSTANCE_TRANSFER) && state.getProperties().get(LaneStateProperty.INSTANCE_ID).getValue().equals(input.from())) {
                                // We joined an instance.
                                System.out.println("Retrieved finish queue 3");
                                ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.INSTANCE_ONLINE,
                                        Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()),
                                                new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                player.setState(newState);
                                player.setQueueRequest(null);
                                player.setInstanceId(input.from());
                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                            } else if (packet.gameId() != null && state.getProperties().containsKey(LaneStateProperty.GAME_ID)
                                    && state.getProperties().get(LaneStateProperty.GAME_ID).getValue().equals(packet.gameId())
                                    && state.getName().equals(LanePlayerState.GAME_TRANSFER)) {
                                System.out.println("Retrieved finish queue 4");
                                // We joined a game.
                                ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.GAME_ONLINE,
                                        Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()),
                                                new ControllerStateProperty(LaneStateProperty.GAME_ID, packet.gameId()),
                                                new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                player.setState(newState);
                                player.setQueueRequest(null);
                                player.setGameId(packet.gameId());
                                player.setInstanceId(input.from());
                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                            } else {
                                System.out.println("Retrieved finish queue 5");
                                // We cannot accept this queue finalization.
                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                            }
                            return;
                        }
                        System.out.println("Retrieved finish queue 6");
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from());
                    }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_STATE), input.from()));
                }, () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
            } else if (iPacket instanceof RequestIdPacket packet) {
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
                    connection.sendPacket(new LongResultPacket(packet.getRequestId(), ResponsePacket.OK, newId), input.from());
                }
            } else if (iPacket instanceof DataObjectReadPacket packet) {
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
            } else if (iPacket instanceof DataObjectWritePacket packet) {
                if (!packet.permissionKey().isIndividual()) {
                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.writeDataObject(packet.permissionKey(), packet.object()).whenComplete((bool, ex) -> {
                    if(ex != null) {
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
            } else if (iPacket instanceof DataObjectRemovePacket packet) {
                if (!packet.permissionKey().isIndividual()) {
                    connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.INVALID_PARAMETERS), input.from());
                }
                dataManager.removeDataObject(packet.permissionKey(), packet.id()).whenComplete((bool, ex) -> {
                    if(ex != null) {
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
            } else if (iPacket instanceof RequestInformationPacket.Player packet) {
                connection.sendPacket(new RequestInformationPacket.PlayerResponse(packet.getRequestId(), ResponsePacket.OK,
                        getPlayer(packet.uuid()).map(ControllerPlayer::convertRecord).orElse(null)), input.from());
            } else if (iPacket instanceof RequestInformationPacket.Players packet) {
                ArrayList<PlayerRecord> data = new ArrayList<>();
                for (ControllerPlayer value : getPlayerManager().getPlayers()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                connection.sendPacket(new RequestInformationPacket.PlayersResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            } else if (iPacket instanceof RequestInformationPacket.Game packet) {
                connection.sendPacket(new RequestInformationPacket.GameResponse(packet.getRequestId(), ResponsePacket.OK,
                        getGame(packet.gameId()).map(ControllerGame::convertRecord).orElse(null)), input.from());
            } else if (iPacket instanceof RequestInformationPacket.Games packet) {
                ArrayList<GameRecord> data = new ArrayList<>();
                for (ControllerGame value : games.values()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                connection.sendPacket(new RequestInformationPacket.GamesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            } else if (iPacket instanceof RequestInformationPacket.Instance packet) {
                connection.sendPacket(new RequestInformationPacket.InstanceResponse(packet.getRequestId(), ResponsePacket.OK,
                        getInstance(packet.id()).map(ControllerLaneInstance::convertRecord).orElse(null)), input.from());
            } else if (iPacket instanceof RequestInformationPacket.Instances packet) {
                ArrayList<InstanceRecord> data = new ArrayList<>();
                for (ControllerLaneInstance value : instances.values()) {
                    // TODO Concurrent?
                    data.add(value.convertRecord());
                }
                connection.sendPacket(new RequestInformationPacket.InstancesResponse(packet.getRequestId(), ResponsePacket.OK, data), input.from());
            } else if (iPacket instanceof SendMessagePacket packet) {
                sendMessage(packet.player(), GsonComponentSerializer.gson().deserialize(packet.message()));
            } else if (iPacket instanceof SavedLocalePacket.Get packet) { // TODO Fix this! All, too many else if
                getPlayerManager().getSavedLocale(packet.player()).whenComplete((locale, ex) -> {
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
            } else if(iPacket instanceof SavedLocalePacket.Set packet) {
                Locale locale = Locale.of(packet.locale());
                getPlayerManager().setSavedLocale(packet.player(), Locale.of(packet.locale())).whenComplete((bool, ex) -> {
                    if(ex != null) {
                        // TODO Additional instnceof? As write?
                        if(ex instanceof IllegalArgumentException) {
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                            return;
                        }
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                        return;
                    }
                    setEffectiveLocale(packet.player(), locale); // TODO On evnet
                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                });
            } else if (iPacket instanceof ResponsePacket<?> response) {
                if (!connection.retrieveResponse(response.getRequestId(), response.transformResult())) {
                    // TODO Well, log about packet that is not wanted.
                }
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

    private ControllerLaneInstance createGetInstance(String id) {
        if (!instances.containsKey(id)) instances.put(id, new ControllerLaneInstance(id, null));
        return instances.get(id);
    }

    public Optional<ControllerLaneInstance> getInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    } // TODO Really public?

    public Collection<ControllerLaneInstance> getInstances() { // TODO Really public?
        return instances.values();
    }

    /**
     * Retrieves the player associated with the given UUID.
     * If the player is not found, an empty {@link Optional} is returned.
     *
     * @param uuid the unique identifier of the player
     * @return an {@link Optional} containing the {@link ControllerPlayer} if found,
     *         or an empty {@link Optional} if the player does not exist
     */
    public Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return getPlayerManager().getPlayer(uuid);
    }

    /**
     * A dynamic way of queuing a player to join an instance or game.
     *
     * @param player            The player
     * @param requestParameters The queue request parameters
     * @return
     */
    public CompletableFuture<Result<Void>> queue(ControllerPlayer player, QueueRequestParameters requestParameters) {
        return queue(player, new QueueRequest(QueueRequestReason.PLUGIN_CONTROLLER, requestParameters));
    }

    /**
     * A dynamic way of queuing a player to join an instance or game.
     * This immediately retrieves the queue request instead of the request parameters.
     * This method is therefore intended to only be used internally, as the request reason should be set by the system.
     *
     * @param player  The player
     * @param request The queue request
     * @return The result of the queuing the request, this does not return the result of the queue
     */
    private CompletableFuture<Result<Void>> queue(ControllerPlayer player, QueueRequest request) {
        player.setQueueRequest(request); // TODO This could override an existing queue, do we want this?. Also check if this happens everywehere.
        QueueStageEvent stageEvent = new QueueStageEvent(player, request);
        handleQueueStage(player, stageEvent);
        return CompletableFuture.completedFuture(new Result<>(ResponsePacket.OK));
    }

    /**
     * Handles a single state of queueing a player which has been initialized by a plugin.
     * This assumes that the new state has already been added to the request.
     *
     * @param player     The player
     * @param stageEvent The event tied to this queue request
     */
    private void handleQueueStage(ControllerPlayer player, QueueStageEvent stageEvent) {
        QueueRequest request = stageEvent.getQueueRequest();
        stageEvent.setNoneResult();
        handleQueueStageEvent(stageEvent);
        QueueStageEventResult result = stageEvent.getResult();
        if (result instanceof QueueStageEventResult.None none) {
            // Queue is being cancelled
            if (none.getMessage() != null) {
                sendMessage(player.getUuid(), none.getMessage());
            }
            player.setQueueRequest(null);
            return;
        } else if (result instanceof QueueStageEventResult.Disconnect disconnect) {
            if (disconnect.getMessage() != null) {
                disconnectPlayer(player.getUuid(), disconnect.getMessage());
            } else {
                disconnectPlayer(player.getUuid(), null);
            }
            player.setQueueRequest(null);
            return;
        } else if (result instanceof QueueStageEventResult.QueueStageEventJoinableResult joinable) {
            ControllerLaneInstance instance;
            String resultInstanceId;
            Long resultGameId;
            // TODO playTogetherPlayers!
            if (joinable instanceof QueueStageEventResult.JoinGame joinGame) {
                resultInstanceId = null;
                resultGameId = joinGame.getGameId();
                Optional<ControllerGame> gameOptional = getGame(joinGame.getGameId());
                if (gameOptional.isEmpty()) {
                    request.stages().add(joinGame.constructStage(QueueStageResult.UNKNOWN_ID));
                    handleQueueStage(player, stageEvent);
                    return;
                }
                ControllerGame game = gameOptional.get();
                Optional<ControllerLaneInstance> instanceOptional = getInstance(game.getInstanceId());
                if (instanceOptional.isEmpty()) {
                    // Run the stage event again to determine a new ID.
                    request.stages().add(joinGame.constructStage(QueueStageResult.UNKNOWN_ID));
                    handleQueueStage(player, stageEvent);
                    return;
                }
                instance = instanceOptional.get();
            } else {
                resultGameId = null;
                QueueStageEventResult.JoinInstance joinInstance = (QueueStageEventResult.JoinInstance) result;
                resultInstanceId = joinInstance.getInstanceId();
                Optional<ControllerLaneInstance> instanceOptional = getInstance(joinInstance.getInstanceId());
                if (instanceOptional.isEmpty()) {
                    // Run the stage event again to determine a new ID.
                    request.stages().add(joinInstance.constructStage(QueueStageResult.UNKNOWN_ID));
                    handleQueueStage(player, stageEvent);
                    return;
                }
                instance = instanceOptional.get();
            }
            Set<UUID> playTogetherPlayers = joinable.getJoinTogetherPlayers();
            if (instance.isJoinable() && instance.isNonPlayable() && instance.getCurrentPlayers() + playTogetherPlayers.size() + 1 <= instance.getMaxPlayers()) {
                // Run the stage event again to find a joinable instance.
                request.stages().add(new QueueStage(QueueStageResult.NOT_JOINABLE, resultInstanceId, resultGameId));
                handleQueueStage(player, stageEvent);
                return;
            }
            // TODO Check whether the game actually has a place left (for 1 + playTogetherPlayers). ONLY WHEN result is JoinGame
            // We found a hopefully free instance, try do send the packet.
            ControllerPlayerState state;
            if (joinable instanceof QueueStageEventResult.JoinGame) {
                state = new ControllerPlayerState(LanePlayerState.GAME_TRANSFER, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()), new ControllerStateProperty(LaneStateProperty.GAME_ID, resultGameId), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
            } else {
                state = new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, instance.getId()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
            }
            player.setState(state); // TODO Better state handling!
            player.setQueueRequest(request);
            CompletableFuture<Result<Void>> future = connection.<Void>sendRequestPacket((id) -> new InstanceJoinPacket(id, player.convertRecord(), false, null), instance.getId()).getFutureResult();
            future.whenComplete((joinPacketResult, exception) -> {
                if (exception != null) {
                    request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, resultInstanceId, resultGameId));
                    handleQueueStage(player, stageEvent);
                    return;
                }
                if (joinPacketResult.isSuccessful()) {
                    CompletableFuture<Result<Void>> joinFuture = joinServer(player.getUuid(), instance.getId());
                    joinFuture.whenComplete((joinResult, joinException) -> {
                        if (joinException != null) {
                            request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, resultInstanceId, resultGameId));
                            handleQueueStage(player, stageEvent);
                            return;
                        }
                        if (!joinResult.isSuccessful()) {
                            // TODO Should we let the Instance know that the player is not joining? Maybe they claimed a spot in the queue.
                            request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, resultInstanceId, resultGameId));
                            handleQueueStage(player, stageEvent);
                            return;
                        }
                        // WOOO, we have joined!, we are done for THIS player

                        if (!playTogetherPlayers.isEmpty()) {
                            QueueRequestParameter partyJoinParameter;
                            if (resultGameId != null) {
                                partyJoinParameter = QueueRequestParameter.create().gameId(resultGameId).instanceId(instance.getId()).build();
                            } else {
                                partyJoinParameter = QueueRequestParameter.create().instanceId(resultInstanceId).build();
                            }
                            QueueRequest partyRequest = new QueueRequest(QueueRequestReason.PARTY_JOIN, QueueRequestParameters.create().add(partyJoinParameter).build());
                            playTogetherPlayers.forEach(uuid -> getPlayer(uuid).ifPresent(controllerPlayer -> queue(controllerPlayer, partyRequest)));
                        }
                    });
                } else {
                    // We are not allowing to join at this instance.
                    request.stages().add(new QueueStage(QueueStageResult.JOIN_DENIED, resultInstanceId, resultGameId));
                    handleQueueStage(player, stageEvent);
                    return;
                }
            });
        }
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
    CompletableFuture<Boolean> updateDataObject(PermissionKey permissionKey, DataObjectId id, Function<DataObject, Boolean> updater) {
        if (!permissionKey.isIndividual()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Permission key is not an individual permission key"));
        }
        return dataManager.updateDataObject(permissionKey, id, updater);
    }

    // TODO Redo
    public void endGame(long id) { // TODO Check
        games.remove(id);
    }

    /**
     * @param owner
     * @param invited
     */
    public void createParty(ControllerPlayer owner, ControllerPlayer invited) {
        if (owner == null || invited == null) return;
        if (owner.getPartyId().isPresent()) return;
        ControllerParty controllerParty = new ControllerParty(System.currentTimeMillis(), owner.getUuid());
        // TODO Check ID for doubles

        controllerParty.sendRequest(invited);
    }

    public void disbandParty(ControllerParty party) { // TODO Redo: might send packets to servers with party info
        if (!parties.containsKey(party.getId())) return;
        parties.remove(party.getId());

        for (UUID uuid : party.getPlayers()) {
            getPlayer(uuid).ifPresent(player -> player.setParty(null));
        }

        party.disband();
    }

    public Collection<ControllerParty> getParties() {
        return parties.values();
    } // TODO Redo

    public Optional<ControllerParty> getParty(long id) {
        return Optional.ofNullable(parties.get(id));
    } // TODO Redo

    public Collection<ControllerGame> getGames() {
        return games.values();
    } // TODO Redo

    public Optional<ControllerGame> getGame(long id) {
        return Optional.ofNullable(games.get(id));
    } // TODO Redo

    /**
     * Method that will switch the server of the given player to the given server.
     * This should not do anything when the player is already connected to the given server.
     *
     * @param uuid        the player's uuid
     * @param destination the server's id
     * @return the completable future with the result
     */
    public abstract CompletableFuture<Result<Void>> joinServer(UUID uuid, String destination);

    /**
     * Gets a new ControllerLaneInstance for the given ControllerPlayer to join.
     * This instance is not intended to be played at a game at currently.
     *
     * @param player  the player requesting a new instance
     * @param exclude the collection of instances to exclude from the output
     * @return the instance to go to, if the optional is null, then no instance could be found
     */
    public abstract Optional<ControllerLaneInstance> getNewInstance(ControllerPlayer player, Collection<ControllerLaneInstance> exclude);

    /**
     * Lets the implemented controller handle the {@link QueueStageEvent}.
     * Do not do blocking actions while handling the event, as often a "direct" response is needed.
     *
     * @param event The event to handle.
     */
    public abstract void handleQueueStageEvent(QueueStageEvent event);

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

}