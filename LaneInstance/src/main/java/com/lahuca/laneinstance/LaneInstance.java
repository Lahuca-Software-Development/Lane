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
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.ReconnectConnection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.connection.request.result.VoidResultPacket;
import com.lahuca.lane.data.manager.PermissionFailedException;
import com.lahuca.lane.events.LaneEvent;
import com.lahuca.lane.records.*;
import com.lahuca.laneinstance.events.InstanceShutdownGameEvent;
import com.lahuca.laneinstance.events.InstanceStartupGameEvent;
import com.lahuca.laneinstance.events.party.*;
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

        dataManager = new InstanceDataManager(this);
        playerManager = new InstancePlayerManager(this, this::sendInstanceStatus, onlineJoinable, playersJoinable, playingJoinable, maxOnlineSlots, maxPlayersSlots, maxPlayingSlots, onlineKickable, playersKickable, playingKickable, isPrivate);
        friendshipManager = new InstanceFriendshipManager(this);

        connection.setOnReconnect(this::sendInstanceStatus);
        connection.initialise(input -> {
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
        return new InstanceRecord(id, type, pm.getReserved(), pm.getOnline(), pm.getPlayers(), pm.getPlaying(),
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
