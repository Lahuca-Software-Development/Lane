/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 18-3-2024 at 14:43 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.packet.data.*;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.connection.request.result.VoidResultPacket;
import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.lane.queue.QueueRequestParameters;
import com.lahuca.lane.records.*;
import com.lahuca.laneinstance.events.InstanceEvent;
import com.lahuca.laneinstance.events.InstanceShutdownGameEvent;
import com.lahuca.laneinstance.events.InstanceStartupGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;
import com.lahuca.laneinstance.retrieval.InstancePartyRetrieval;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * The root endpoint for most calls of methods for a LaneInstance.
 */
public abstract class LaneInstance implements RecordConverter<InstanceRecord> {

    private static LaneInstance instance;

    public static LaneInstance getInstance() { // TODO Optional?
        return instance;
    }

    private final String id;
    private final ReconnectConnection connection;
    private String type;

    private final InstancePlayerManager playerManager;

    private final HashMap<Long, InstanceGame> games = new HashMap<>();

    public LaneInstance(String id, ReconnectConnection connection, String type, boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots, boolean onlineKickable, boolean playersKickable, boolean playingKickable, boolean isPrivate) throws IOException, InstanceInstantiationException {
        if (instance != null) throw new InstanceInstantiationException();
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(connection, "connection cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        instance = this;
        this.id = id;
        this.type = type;

        Packet.registerPackets();

        this.connection = connection;

        playerManager = new InstancePlayerManager(this, this::sendInstanceStatus, onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable, isPrivate);

        connection.setOnReconnect(this::sendInstanceStatus);
        connection.initialise(input -> {
            switch (input.packet()) {
                case InstanceJoinPacket packet -> {
                    // TODO This all assumes we do all party members one at a time!!
                    // Check if we can even join that queue type
                    HashSet<LanePlayer> kickable = null;
                    Map<UUID, Integer> playerMap = Map.of(packet.player().uuid(), packet.player().queuePriority());
                    Set<UUID> playerSet = Set.of(packet.player().uuid());
                    if(!getPlayerManager().hasQueueSlots(playerSet, packet.queueType())) {
                        // Cannot grant slot yet, check if we even can join
                        if(!getPlayerManager().isQueueJoinable(packet.queueType())) {
                            // We cannot join the queue type
                            sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                            return;
                        }
                        // Okay, check if we can kick someone
                        kickable = getPlayerManager().findKickableLanePlayers(playerMap, packet.queueType(), packet.gameId(), getPlayerManager()::getInstancePlayer);
                        if(kickable == null) {
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
                        if(!game.hasQueueSlots(playerSet, packet.queueType())) {
                            // Cannot grant slot yet, check if we even can join
                            if(!game.isQueueJoinable(packet.queueType())) {
                                // We cannot join the queue type
                                sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                                return;
                            }
                            // Okay, so we need to kick someone
                            if(kickable == null) {
                                // So we kick someone
                                kickable = getPlayerManager().findKickableLanePlayers(playerMap, packet.queueType(), packet.gameId(), getPlayerManager()::getInstancePlayer);
                                if(kickable == null) {
                                    // We could not find a slot
                                    sendSimpleResult(packet, ResponsePacket.NO_FREE_SLOTS);
                                    return;
                                }
                            }
                            // So if there was already someone found, they will be kicked
                        }
                    }
                    if(kickable != null) {
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
                case ResponsePacket<?> response -> {
                    if (!connection.retrieveResponse(response.getRequestId(), response.toObjectResponsePacket())) {
                        // TODO Handle output: failed response
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + input.packet());
            }
        });
        sendInstanceStatus();
    }

    public String getId() {
        return id;
    }

    public void shutdown() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        HashSet<Long> gamesSet = new HashSet<>(games.keySet());
        gamesSet.forEach(gameId -> futures.add(unregisterGame(gameId)));
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        connection.disableReconnect();
        connection.close();
        // TODO Probably other stuff?
    }

    public ReconnectConnection getConnection() {
        return connection;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        sendInstanceStatus();
    }

    public InstancePlayerManager getPlayerManager() {
        return playerManager;
    }

    public abstract void disconnectPlayer(UUID player, Component message);

    public void sendController(Packet packet) {
        connection.sendPacket(packet, null);
    }

    private void sendSimpleResult(long requestId, String result) {
        sendController(new VoidResultPacket(requestId, result));
    }

    private void sendSimpleResult(RequestPacket request, String result) {
        sendSimpleResult(request.getRequestId(), result);
    }

    private static <T> CompletableFuture<T> simpleException(String result) { // TODO Move
        return CompletableFuture.failedFuture(new UnsuccessfulResultException(result));
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(convertRecord()));
    }

    /**
     * Request the given player to be queued with the given parameters.
     *
     * @param playerId          the player's uuid
     * @param requestParameters the queue request parameters
     * @return a {@link CompletableFuture} with a void to signify success: the player has been queued
     */
    public CompletableFuture<Void> queue(UUID playerId, QueueRequestParameters requestParameters) {
        return connection.<Void>sendRequestPacket(id -> new QueueRequestPacket(id, playerId, requestParameters), null).getResult();
    }

    private CompletableFuture<Long> requestId(RequestIdPacket.Type idType) {
        if (idType == null) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<Long>sendRequestPacket(id -> new RequestIdPacket(id, idType), null).getResult();
    }

    /**
     * Registers a new game upon the function that gives a new game ID.
     *
     * @param gameConstructor the id to game parser. Preferably, a lambda with LaneGame::new is given, whose constructor consists of only the ID.
     * @return a {@link CompletableFuture} of the registering, it completes successfully with the game when it is successfully registered.
     */
    public <T extends InstanceGame> CompletableFuture<T> registerGame(Function<Long, T> gameConstructor) {
        if (gameConstructor == null) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        // First request new ID
        return requestId(RequestIdPacket.Type.GAME).thenCompose(gameId -> {
            // We got a new ID, construct game
            T game = gameConstructor.apply(gameId);
            if (game.getGameId() != gameId || games.containsKey(game.getGameId()) && !game.getInstanceId().equals(id)) {
                throw new UnsuccessfulResultException(ResponsePacket.INVALID_ID);
            }
            // Include the game and send the update packet
            games.put(game.getGameId(), game);
            return connection.<Void>sendRequestPacket(id -> new GameStatusUpdatePacket(id, game.convertRecord()), null).getResult().handle((data, ex) -> {
                if (ex != null) {
                    // Oh, the update isn't sent, remove the game
                    games.remove(game.getGameId());
                    throw new CompletionException(ex);
                }
                // Update sent, we are done, start up, return the new game
                game.onStartup();
                handleInstanceEvent(new InstanceStartupGameEvent(game));
                return game;
            });
        });
    }

    /**
     * Unregisters the given game.
     * Calls the {@link InstanceGame#onShutdown()} function following by calling a {@link InstanceShutdownGameEvent}.
     * The controller will queue all players if they are not yet in a queue.
     * @param gameId the game's ID
     * @return a {@link CompletableFuture} with a void to signify success: it has been unregistered
     */
    public CompletableFuture<Void> unregisterGame(long gameId) {
        InstanceGame game = games.get(gameId);
        if(game == null) {
            throw new IllegalStateException("No game with the given game ID found on this instance");
        }
        try {
            game.onShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        handleInstanceEvent(new InstanceShutdownGameEvent(game));
        games.remove(gameId);
        return connection.<Void>sendRequestPacket(id -> new GameShutdownPacket(id, gameId), null).getResult(); // TODO What if failed??
    }

    /**
     * Reads a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to retrieve the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a {@link Optional} with the data object as value if present
     */
    public CompletableFuture<Optional<DataObject>> readDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<DataObject>sendRequestPacket(requestId -> new DataObjectReadPacket(requestId, id, permissionKey), null).getResult().thenApply(Optional::ofNullable);
    }

    /**
     * Writes a data object at the given id with the provided permission key.
     * This either creates or updates the data object.
     *
     * @param object        the id of the data object
     * @param permissionKey the permission key that wants to write the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a void to signify success: the data object has been written
     */
    public CompletableFuture<Void> writeDataObject(DataObject object, PermissionKey permissionKey) {
        if (object == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<Void>sendRequestPacket(requestId -> new DataObjectWritePacket(requestId, object, permissionKey), null).getResult();
    }

    /**
     * Removes a data object at the given id with the provided permission key.
     *
     * @param id            the id of the data object
     * @param permissionKey the permission key that wants to remove the data object, this must be an individual key
     * @return a {@link CompletableFuture} with a void to signify success: the data object has been removed
     */
    public CompletableFuture<Void> removeDataObject(DataObjectId id, PermissionKey permissionKey) {
        if (id == null || permissionKey == null || !permissionKey.isFormattedCorrectly())
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<Void>sendRequestPacket(requestId -> new DataObjectRemovePacket(requestId, id, permissionKey), null).getResult();
    }

    // TODO updateDataObject

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
    public @NotNull CompletableFuture<ArrayList<DataObjectId>> listDataObjectIds(DataObjectId prefix) {
        if(id == null || prefix == null) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<ArrayList<DataObjectId>>sendRequestPacket(requestId -> new DataObjectListIdsPacket(requestId, prefix), null).getResult();
    }

    /**
     * Retrieves a list of DataObjects from the given table that match the version.
     *
     * @param prefix        the prefix ID. This cannot be null, its values can be null.
     * @param permissionKey the permission key to use while reading and writing
     * @param version       the version to match, null if no version is required
     * @return a {@link CompletableFuture} with the array of DataObjects matching the version
     */
    public @NotNull CompletableFuture<ArrayList<DataObject>> listDataObjects(@NotNull DataObjectId prefix, PermissionKey permissionKey, Integer version) {
        if(id == null || prefix == null || permissionKey == null || !permissionKey.isFormattedCorrectly()) return simpleException(ResponsePacket.INVALID_PARAMETERS);
        return connection.<ArrayList<DataObject>>sendRequestPacket(requestId -> new DataObjectsListPacket(requestId, prefix, permissionKey, version), null).getResult();
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
        if (id == null || permissionKey == null || !permissionKey.isFormattedCorrectly() || sourceId == null || targetId == null) {
            return simpleException(ResponsePacket.INVALID_PARAMETERS);
        }
        return connection.<Void>sendRequestPacket(requestId -> new DataObjectCopyPacket(requestId, permissionKey, sourceId, targetId), null).getResult();
    }

    // TODO Maybe still delagate getInstancePLayer()?

    /**
     * Retrieves an InstanceGame by a given game ID on this instance.
     *
     * @param gameId the game ID of the game
     * @return an optional of the LaneGame object.
     */
    public Optional<InstanceGame> getInstanceGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * Retrieves a collection of all Lane games on this instance.
     *
     * @return the collection
     */
    public Collection<InstanceGame> getInstanceGames() {
        return games.values();
    }

    /**
     * Retrieves a game record of the game with the given ID on the controller.
     * The value is null when no game with the given ID is present.
     *
     * @param gameId the ID of the game
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will be the {@link GameRecord} if present
     */
    public CompletableFuture<Optional<GameRecord>> getGameRecord(long gameId) {
        return connection.<Optional<GameRecord>>sendRequestPacket(id -> new RequestInformationPacket.Game(id, gameId), null).getResult();
    }

    /**
     * Retrieves a collection of immutable records of all games on the controller.
     *
     * @return a {@link CompletableFuture} with the game records
     */
    public CompletableFuture<ArrayList<GameRecord>> getAllGameRecords() {
        return connection.<ArrayList<GameRecord>>sendRequestPacket(RequestInformationPacket.Games::new, null).getResult();
    }

    /**
     * Retrieves the instance record of the instance with the given ID on the controller.
     * The value is null when no instance with the given ID is present.
     *
     * @param id the ID of the instance
     * @return a {@link CompletableFuture} with a {@link Optional} whose value is the {@link InstanceRecord} if present
     */
    public CompletableFuture<Optional<InstanceRecord>> getInstanceRecord(String id) {
        Objects.requireNonNull(id, "id cannot be null");
        return connection.<InstanceRecord>sendRequestPacket(requestId -> new RequestInformationPacket.Instance(requestId, id), null).getResult().thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves a collection of immutable records of all instances on the controller.
     *
     * @return a {@link CompletableFuture} with the instance records
     */
    public CompletableFuture<ArrayList<InstanceRecord>> getAllInstanceRecords() {
        return connection.<ArrayList<InstanceRecord>>sendRequestPacket(RequestInformationPacket.Instances::new, null).getResult();
    }

    @Override
    public InstanceRecord convertRecord() {
        InstancePlayerManager pm = getPlayerManager();
        return new InstanceRecord(id, type, pm.getReserved(), pm.getOnline(), pm.getPlayers(), pm.getPlaying(),
                pm.isOnlineJoinable(), pm.isPlayersJoinable(), pm.isPlayingJoinable(),
                pm.getMaxOnlineSlots(), pm.getMaxPlayersSlots(), pm.getMaxPlayingSlots(),
                pm.isOnlineKickable(), pm.isPlayersKickable(), pm.isPlayingKickable(), pm.isPrivate());
    }

    /**
     * Retrieves a party from the instance, identified by the given party ID.
     * This retrieval object is not necessary up to date, it is recommended to not store its output for too long.
     *
     * @param partyId the party ID
     * @return a {@link CompletableFuture} that completes with the retrieval of the party
     */
    public CompletableFuture<InstancePartyRetrieval> getParty(long partyId) {
        return connection.<PartyRecord>sendRequestPacket(id -> new PartyPacket.Retrieve.Request(id, partyId), null).getResult()
                .thenApply(InstancePartyRetrieval::new);
    }

    /**
     * Retrieves the profile data of the profile identified by the given UUID.
     * @param uuid the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will be the profile data if present
     */
    public CompletableFuture<Optional<InstanceProfileData>> getProfileData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return connection.<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.GetProfileData(id, uuid), null).getResult()
                .thenApply(Optional::ofNullable).thenApply(opt -> opt.map(InstanceProfileData::new));
    }

    /**
     * Creates a new profile given the profile type.
     * This stores the profile information at a new profile UUID.
     * @param type the profile type
     * @return a {@link CompletableFuture} with a {@link UUID}, which is the UUID of the new profile
     */
    public CompletableFuture<InstanceProfileData> createNewProfile(ProfileType type) {
        Objects.requireNonNull(type, "type cannot be null");
        return connection.<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.CreateNew(id, type), null).getResult().thenApply(InstanceProfileData::new);
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
    public @NotNull CompletableFuture<InstanceProfileData> createSubProfile(@NotNull InstanceProfileData current, @NotNull ProfileType type, @NotNull String name, boolean active) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (type == ProfileType.NETWORK) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot add a network profile as sub profile"));
        }
        return connection.<ProfileRecord>sendRequestPacket(id -> new ProfilePacket.CreateSubProfile(id, current.getId(), type, name, active), null).getResult()
                .thenApply(profile -> {
                    if(profile != null) {
                        current.addSubProfile(profile.id(), name, active);
                    }
                    return new InstanceProfileData(profile);
                });
    }

    /**
     * Adds a sub profile to another "super profile", the current profile at the given name with the given active state.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been added or not.
     * These changes are reflected in the respective parameters' values as well; due to the implementation, this might not be the most up-to-date data.
     * The sub profile cannot be of type {@link ProfileType#NETWORK}.
     * If the sub profile is of type {@link ProfileType#SUB}, it cannot have a super profile yet.
     * If the sub profile already exists at the given name and profile, it still updates the active state.
     *
     * @param current    the current profile, where to add the sub profile to
     * @param subProfile the sub profile to add
     * @param name       the name to add the sub profile to
     * @param active whether the sub profile is active
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been added, {@code false} otherwise
     */
    public CompletableFuture<Boolean> addSubProfile(InstanceProfileData current, InstanceProfileData subProfile, String name, boolean active) {
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
        return connection.<Boolean>sendRequestPacket(id -> new ProfilePacket.AddSubProfile(id, current.getId(), subProfile.getId(), name, active), null).getResult()
                .thenApply(status -> {
                    if(status) {
                        subProfile.addSuperProfile(current.getId());
                        current.addSubProfile(subProfile.getId(), name, active);
                    }
                    return status;
                });
    }

    /**
     * Removes a sub profile from another "super profile", the current profile at the given name.
     * If the name is null, the sub profile is removed at all positions in the super profile.
     * This returns a {@link CompletableFuture} with the result whether the sub profile has been removed or not.
     * These changes are reflected in the respective parameters' values as well; due to the implementation, this might not be the most up-to-date data.
     *
     * @param current    the current profile, where to remove the sub profile from
     * @param subProfile the sub profile to remove
     * @param name       the name to remove the sub profile from, or null to completely remove it
     * @return a {@link CompletableFuture} with a boolean: {@code true} if the sub profile has been removed, {@code false} otherwise
     */
    public CompletableFuture<Boolean> removeSubProfile(InstanceProfileData current, InstanceProfileData subProfile, String name) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(subProfile, "subProfile cannot be null");
        return connection.<Boolean>sendRequestPacket(id -> new ProfilePacket.RemoveSubProfile(id, current.getId(), subProfile.getId(), name), null).getResult()
                .thenApply(status -> {
                    if(status) {
                        current.removeSubProfile(subProfile.getId(), name);
                        subProfile.removeSuperProfile(current.getId());
                    }
                    return status;
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
    public CompletableFuture<Void> resetDeleteProfile(InstanceProfileData current, boolean delete) {
        Objects.requireNonNull(current, "current cannot be null");
        if (delete && (!current.getSuperProfiles().isEmpty() || !current.getSubProfiles().isEmpty())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot delete profile with sub or super profiles"));
        }
        return connection.<Void>sendRequestPacket(id -> new ProfilePacket.ResetDelete(id, current.getId(), delete), null).getResult();
    }

    /**
     * Copies one profile to another, see {@link ProfileData#copyProfile(ProfileData)}, this does not copy the profile data information object.
     *
     * @param current the profile to copy to
     * @param from the profile to copy from
     * @return a {@link CompletableFuture} with a void to signify success: it has been copied completely
     */
    public CompletableFuture<Void> copyProfile(InstanceProfileData current, ProfileData from) {
        Objects.requireNonNull(current, "current cannot be null");
        Objects.requireNonNull(from, "from cannot be null");
        return connection.<Void>sendRequestPacket(id -> new ProfilePacket.Copy(id, current.getId(), from.getId()), null).getResult();
    }

    /**
     * Sets the network profile of the given player to the provided profile.
     * The profile must be of type {@link ProfileType#NETWORK}.
     *
     * @param player the player
     * @param profile the profile
     * @return a {@link CompletableFuture} to signify success: the profile has been set
     */
    public CompletableFuture<Void> setNetworkProfile(InstancePlayer player, InstanceProfileData profile) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        // Check if already set
        if(player.getNetworkProfileUuid().equals(profile.getId())) return CompletableFuture.completedFuture(null);
        // Check whether the profile can be set
        if(profile.getType() != ProfileType.NETWORK || !profile.getSuperProfiles().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Can only set a network profile with no super profiles"));
        }
        return connection.<Void>sendRequestPacket(id -> new ProfilePacket.SetNetworkProfile(id, player.getUuid(), profile.getId()), null).getResult();
    }

    /**
     * Sets the nickname of the given player.
     *
     * @param player the player
     * @param nickname the nickname
     * @return a {@link CompletableFuture} to signify success: the nickname has been set
     */
    public CompletableFuture<Void> setNickname(@NotNull InstancePlayer player, @NotNull String nickname) {
        // Check if already set
        if(player.getNickname().equals(nickname)) return CompletableFuture.completedFuture(null);
        return connection.<Void>sendRequestPacket(id -> new SetInformationPacket.PlayerSetNickname(id, player.getUuid(), nickname), null).getResult();
    }

    /**
     * Lets the implemented instance handle the Lane Instance event.
     * Some events have results tied to them, which are to be expected to return in the CompletableFuture.
     * Some events might not have the possibility to wait for the result asynchronously, so that the CompletableFuture is waited for.
     *
     * @param event the event to handle
     * @param <E>   the Lane instance event type
     * @return the CompletableFuture with the modified event
     */
    public abstract <E extends InstanceEvent> CompletableFuture<E> handleInstanceEvent(E event);

    /**
     * Lets the implemented instance run the given runnable on the main thread.
     * @param runnable the runnable
     */
    public abstract void runOnMainThread(Runnable runnable);

    public abstract void updatePlayerListName(UUID uuid);

}
