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
import com.lahuca.lane.connection.request.ResultUnsuccessfulException;
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
import com.lahuca.lane.records.GameRecord;
import com.lahuca.lane.records.GameStateRecord;
import com.lahuca.lane.records.InstanceRecord;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lanecontroller.events.QueueStageEvent;
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

    private final Connection connection;
    private final DataManager dataManager;

    private final ControllerPlayerManager playerManager;
    private final ControllerPartyManager partyManager;


    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances


    public Controller(Connection connection, DataManager dataManager) throws IOException {
        instance = this;
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
                case GameStatusUpdatePacket(long requestId, long gameId, String name, GameStateRecord state) -> {
                    if (!games.containsKey(gameId)) {
                        // A new game has been created, yeey!
                        ControllerGameState initialState = new ControllerGameState(state);
                        games.put(gameId, new ControllerGame(gameId, input.from(), name, initialState));
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                        return;
                    }
                    ControllerGame game = games.get(gameId);
                    if (!game.getInstanceId().equals(input.from())) {
                        connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                        return;
                    }
                    games.get(gameId).update(name, state);
                    connection.sendPacket(new VoidResultPacket(requestId, ResponsePacket.OK), input.from());
                }
                case InstanceStatusUpdatePacket packet -> createGetInstance(input.from()).applyRecord(packet.record());

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
                case PartyPacket.Disband packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.disband()), input.from()),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.SetOwner packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        getPlayerManager().getPlayer(packet.player()).ifPresentOrElse(player ->
                                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), ResponsePacket.OK, party.setOwner(player)), input.from()),
                                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from())),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));
                case PartyPacket.Warp packet ->
                        getPartyManager().getParty(packet.partyId()).ifPresentOrElse(party ->
                                        connection.sendPacket(new SimpleResultPacket<>(packet.getRequestId(), party.warpParty()), input.from()),
                                () -> connection.sendPacket(new SimpleResultPacket<Boolean>(packet.getRequestId(), ResponsePacket.INVALID_ID), input.from()));

                case QueueRequestPacket packet ->
                        getPlayer(packet.player()).ifPresentOrElse(player -> player.queue(packet.parameters()).whenComplete((result, exception) -> {
                            String response = ResponsePacket.OK;
                            if (exception != null) {
                                if (exception instanceof ResultUnsuccessfulException ex) {
                                    response = ex.getMessage();
                                } else {
                                    response = ResponsePacket.UNKNOWN;
                                }
                            }
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), response), input.from());
                        }), () -> connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.INVALID_PLAYER), input.from()));
                case QueueFinishedPacket packet -> {
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
                                    ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.INSTANCE_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
                                    player.setState(newState);
                                    player.setQueueRequest(null);
                                    player.setInstanceId(input.from());
                                    connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                                } else if (packet.gameId() != null && state.getProperties().containsKey(LaneStateProperty.GAME_ID) && state.getProperties().get(LaneStateProperty.GAME_ID).getValue().equals(packet.gameId()) && state.getName().equals(LanePlayerState.GAME_TRANSFER)) {
                                    System.out.println("Retrieved finish queue 4");
                                    // We joined a game.
                                    ControllerPlayerState newState = new ControllerPlayerState(LanePlayerState.GAME_ONLINE, Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, input.from()), new ControllerStateProperty(LaneStateProperty.GAME_ID, packet.gameId()), new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())));
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
                case SendMessagePacket packet -> {
                    sendMessage(packet.player(), GsonComponentSerializer.gson().deserialize(packet.message()));
                }

                case SavedLocalePacket.Get packet -> {
                    getPlayerManager().getSavedLocale(packet.player()).whenComplete((locale, ex) -> {
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
                    Locale locale = Locale.of(packet.locale());
                    getPlayerManager().setSavedLocale(packet.player(), Locale.of(packet.locale())).whenComplete((bool, ex) -> {
                        if (ex != null) {
                            // TODO Additional instnceof? As write?
                            if (ex instanceof IllegalArgumentException) {
                                connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.ILLEGAL_ARGUMENT), input.from());
                                return;
                            }
                            connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.UNKNOWN), input.from());
                            return;
                        }
                        setEffectiveLocale(packet.player(), locale); // TODO On evnet
                        connection.sendPacket(new VoidResultPacket(packet.getRequestId(), ResponsePacket.OK), input.from());
                    });
                }
                case ResponsePacket<?> response -> {
                    if (!connection.retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
                        // TODO Well, log about packet that is not wanted.
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + iPacket);
            }
            ;
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
    public abstract CompletableFuture<Void> joinServer(UUID uuid, String destination);

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