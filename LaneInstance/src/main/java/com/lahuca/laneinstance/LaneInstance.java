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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.ResponseError;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.ResponseErrorException;
import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lane.records.*;
import com.lahuca.laneinstance.events.InstanceShutdownGameEvent;
import com.lahuca.laneinstance.events.InstanceStartupGameEvent;
import com.lahuca.laneinstance.game.InstanceGame;
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
    private final String gameAddress;
    private final int gameAddressPort;
    private final ReconnectConnection connection;
    private String type;

    private final InstanceDataManager dataManager;
    private final InstancePlayerManager playerManager;
    private final InstanceFriendshipManager friendshipManager;

    private final HashMap<Long, InstanceGame> games = new HashMap<>();

    private final Cache<Long, InstanceParty> partyReplicas = Caffeine.newBuilder()
            .weakValues()
            .removalListener((RemovalListener<Long, InstanceParty>) (key, value, cause) -> {
                if (cause != RemovalCause.REPLACED && value != null) {
                    value.unsubscribeReplicated();
                }
            })
            .build();

    public LaneInstance(String id, String gameAddress, int gameAddressPort, ReconnectConnection connection, String type, boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots, boolean onlineKickable, boolean playersKickable, boolean playingKickable, boolean isPrivate) throws IOException, InstanceInstantiationException {
        if (instance != null) throw new InstanceInstantiationException();
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(connection, "connection cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        instance = this;
        this.id = id;
        this.gameAddress = gameAddress;
        this.gameAddressPort = gameAddressPort;
        this.type = type;

        Packet.registerPackets();

        this.connection = connection;

        dataManager = new InstanceDataManager(this);
        playerManager = new InstancePlayerManager(this, this::sendInstanceStatus, onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable, isPrivate);
        friendshipManager = new InstanceFriendshipManager(this);

        connection.setOnReconnect(() -> {
            sendInstanceStatus();
            Collection<InstanceGame> gamesCopy = new ArrayList<>(getInstanceGames());
            gamesCopy.forEach(game -> {
                // Try to update the game to the controller
                connection.<Void>sendRequestPacket(requestId -> new GameStatusUpdatePacket(requestId, game.convertRecord()), null).getResult().whenComplete((data, ex) -> {
                    if (ex != null) {
                        // Oh, the update isn't sent, remove the game
                        unregisterGame(game.getGameId());
                        return;
                    }
                    // Update sent, we are done
                }).join(); // TODO sync?
            });
        });
        connection.initialise(new InstanceInputPacket(this, partyReplicas));
        sendInstanceStatus();
    }

    public String getId() {
        return id;
    }

    public String getGameAddress() {
        return gameAddress;
    }

    public int getGameAddressPort() {
        return gameAddressPort;
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

    public InstanceDataManager getDataManager() {
        return dataManager;
    }

    public InstancePlayerManager getPlayerManager() {
        return playerManager;
    }

    public InstanceFriendshipManager getFriendshipManager() {
        return friendshipManager;
    }

    public abstract void disconnectPlayer(UUID player, Component message);

    public void sendController(Packet packet) {
        connection.sendPacket(packet, null);
    }

    private void sendInstanceStatus() {
        sendController(new InstanceStatusUpdatePacket(convertRecord()));
    }

    private CompletableFuture<Long> requestId(RequestIdPacket.Type idType) {
        if (idType == null) return ResponseError.ILLEGAL_ARGUMENT.failedFuture();
        return connection.<Long>sendRequestPacket(id -> new RequestIdPacket(id, idType), null).getResult();
    }

    /**
     * Registers a new game upon the function that gives a new game ID.
     *
     * @param gameConstructor the id to game parser. Preferably, a lambda with LaneGame::new is given, whose constructor consists of only the ID.
     * @return a {@link CompletableFuture} of the registering, it completes successfully with the game when it is successfully registered.
     */
    public <T extends InstanceGame> CompletableFuture<T> registerGame(Function<Long, T> gameConstructor) {
        if (gameConstructor == null) return ResponseError.ILLEGAL_ARGUMENT.failedFuture();
        // First request new ID
        return requestId(RequestIdPacket.Type.GAME).thenCompose(gameId -> {
            // We got a new ID, construct game
            T game = gameConstructor.apply(gameId);
            if (game.getGameId() != gameId || games.containsKey(game.getGameId()) && !game.getInstanceId().equals(id)) {
                throw ResponseError.INVALID_ID.exception();
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
     *
     * @param gameId the game's ID
     * @return a {@link CompletableFuture} with a void to signify success: it has been unregistered
     */
    public CompletableFuture<Void> unregisterGame(long gameId) {
        InstanceGame game = games.get(gameId);
        if (game == null) {
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
        return new InstanceRecord(id, gameAddress, gameAddressPort, type, pm.getReserved(), pm.getOnline(), pm.getPlayers(), pm.getPlaying(),
                pm.isOnlineJoinable(), pm.isPlayersJoinable(), pm.isPlayingJoinable(),
                pm.getMaxOnlineSlots(), pm.getMaxPlayersSlots(), pm.getMaxPlayingSlots(),
                pm.isOnlineKickable(), pm.isPlayersKickable(), pm.isPlayingKickable(), pm.isPrivate());
    }

    /**
     * Retrieves a party from the instance, identified by the given party ID.
     *
     * @param partyId the party ID
     * @return a {@link CompletableFuture} that completes with an optional with the party
     */
    public CompletableFuture<Optional<InstanceParty>> getParty(long partyId) {
        InstanceParty cached = partyReplicas.getIfPresent(partyId);
        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));
        return connection.<PartyRecord>sendRequestPacket(id -> new PartyPacket.Retrieve.Request(id, partyId), null).getResult()
                .thenApply(data -> {
                    if (data == null) return Optional.empty();
                    InstanceParty party = new InstanceParty(data);
                    partyReplicas.put(party.getId(), party);
                    return Optional.of(party);
                });
    }

    /**
     * Retrieves a party from the instance, that belongs to the given player can also create a new one.
     *
     * @param uuid the player's UUID
     * @return a {@link CompletableFuture} that completes with an optional with the party
     */
    public CompletableFuture<Optional<InstanceParty>> getPlayerParty(UUID uuid, boolean createIfNeeded) {
        Long partyId = getPlayerManager().getInstancePlayer(uuid).flatMap(InstancePlayer::getPartyId).orElse(null);
        if (partyId != null) return getParty(partyId);
        return connection.<PartyRecord>sendRequestPacket(id -> new PartyPacket.Retrieve.RequestPlayerParty(id, uuid, createIfNeeded), null).getResult()
                .thenApply(data -> {
                    if (data == null) return Optional.empty();
                    InstanceParty party = new InstanceParty(data);
                    partyReplicas.put(party.getId(), party);
                    return Optional.of(party);
                });
    }

    /**
     * Creates a party with the given owner.
     * This can only be done with the given owner is not yet in a party.
     *
     * @param owner the owner
     * @return a {@link CompletableFuture} with the party, the {@link Optional} is null when the party could not be made or if the player was already in one
     * @throws IllegalArgumentException when {@code owner} is null
     */
    public CompletableFuture<Optional<InstanceParty>> createParty(InstancePlayer owner) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        if (owner.getPartyId().isPresent()) return CompletableFuture.completedFuture(Optional.empty());
        return connection.<PartyRecord>sendRequestPacket(id -> new PartyPacket.Operations.Create(id, owner.getUuid()), null).getResult()
                .thenApply(data -> {
                    if (data == null) return Optional.empty();
                    InstanceParty party = new InstanceParty(data);
                    partyReplicas.put(party.getId(), party);
                    return Optional.of(party);
                });
    }

    /**
     * Sets the nickname of the given player.
     *
     * @param player   the player
     * @param nickname the nickname
     * @return a {@link CompletableFuture} to signify success: the nickname has been set
     */
    public CompletableFuture<Void> setNickname(@NotNull InstancePlayer player, @NotNull String nickname) {
        // Check if already set
        if (Objects.equals(player.getNickname().orElse(null), nickname)) return CompletableFuture.completedFuture(null);
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
    public abstract <E extends LaneEvent> CompletableFuture<E> handleInstanceEvent(E event);

    /**
     * Lets the implemented instance run the given runnable on the main thread.
     *
     * @param runnable the runnable
     */
    public abstract void runOnMainThread(Runnable runnable);

    public abstract void updatePlayerListName(UUID uuid);

}
