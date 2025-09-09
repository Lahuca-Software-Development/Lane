package com.lahuca.laneinstance;

import com.lahuca.lane.connection.packet.GameQuitPacket;
import com.lahuca.lane.connection.packet.QueueFinishedPacket;
import com.lahuca.lane.connection.packet.RequestInformationPacket;
import com.lahuca.lane.connection.packet.data.SavedLocalePacket;
import com.lahuca.lane.connection.request.UnsuccessfulResultException;
import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.lane.game.Slottable;
import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.laneinstance.events.*;
import com.lahuca.laneinstance.game.InstanceGame;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class InstancePlayerManager implements Slottable {

    private final LaneInstance instance;
    private final Runnable sendInstanceStatus; // TODO Should this not give an CompletableFuture back?

    private final ConcurrentHashMap<UUID, InstancePlayer> reserved = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> online = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> players = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> playing = ConcurrentHashMap.newKeySet();

    private boolean onlineJoinable;
    private boolean playersJoinable;
    private boolean playingJoinable;
    private int maxOnlineSlots;
    private int maxPlayersSlots;
    private int maxPlayingSlots;
    private boolean onlineKickable;
    private boolean playersKickable;
    private boolean playingKickable;
    private boolean isPrivate;

    InstancePlayerManager(LaneInstance instance, Runnable sendInstanceStatus, boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots, boolean onlineKickable, boolean playersKickable, boolean playingKickable, boolean isPrivate) {
        this.instance = instance;
        this.sendInstanceStatus = sendInstanceStatus;
        this.onlineJoinable = onlineJoinable;
        this.playersJoinable = playersJoinable;
        this.playingJoinable = playingJoinable;
        this.maxOnlineSlots = maxOnlineSlots;
        this.maxPlayersSlots = maxPlayersSlots;
        this.maxPlayingSlots = maxPlayingSlots;
        this.onlineKickable = onlineKickable;
        this.playersKickable = playersKickable;
        this.playingKickable = playingKickable;
        this.isPrivate = isPrivate;
    }

    /**
     * Retrieves an InstancePlayer by a given UUID on this instance.
     *
     * @param player the UUID of the player
     * @return an optional of the InstancePlayer object.
     */
    public Optional<InstancePlayer> getInstancePlayer(UUID player) {
        return Optional.ofNullable(reserved.get(player));
    }

    /**
     * Retrieves a collection of all instance players on this instance.
     *
     * @return the collection
     */
    public Collection<InstancePlayer> getInstancePlayers() {
        return Collections.unmodifiableCollection(reserved.values());
    }

    /**
     * Retrieves the player record of the player with the given UUID on the controller.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} with the optional of the player record
     */
    public CompletableFuture<Optional<PlayerRecord>> getPlayerRecord(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return instance.getConnection().<PlayerRecord>sendRequestPacket(id -> new RequestInformationPacket.Player(id, uuid), null).getResult()
                .thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves a collection of immutable records of all players on the controller.
     *
     * @return a {@link CompletableFuture} with the player records
     */
    public CompletableFuture<ArrayList<PlayerRecord>> getAllPlayerRecords() {
        return instance.getConnection().<ArrayList<PlayerRecord>>sendRequestPacket(RequestInformationPacket.Players::new, null).getResult();
    }

    /**
     * Gets the last known username of the player with the given UUID.
     * It is taken either immediately if the player is online from the controller, otherwise the value that is present in the data manager.
     *
     * @param uuid the player's UUID.
     * @return a {@link CompletableFuture} with an {@link Optional}, if data has been found the optional is populated with the username; otherwise it is empty.
     */
    public CompletableFuture<Optional<String>> getPlayerUsername(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return instance.getConnection().<String>sendRequestPacket(id -> new RequestInformationPacket.PlayerUsername(id, uuid), null)
                .getResult().thenApply(Optional::ofNullable);
    }

    /**
     * Gets the last known UUID of the player with the given username.
     * It is taken either immediately if the player is online from the controller, otherwise the value that is present in the data manager.
     *
     * @param username the player's username
     * @return a {@link CompletableFuture} with an {@link Optional}, if data has been found, the optional is populated with the UUID; otherwise it is empty
     */
    public CompletableFuture<Optional<UUID>> getPlayerUuid(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        return instance.getConnection().<String>sendRequestPacket(id -> new RequestInformationPacket.PlayerUuid(id, username), null)
                .getResult().thenApply(val -> val == null ? Optional.empty() : Optional.of(UUID.fromString(val)));
    }

    private void disconnectPlayer(UUID uuid, Component message) {
        instance.disconnectPlayer(uuid, message); // TODO Move to here? Maybe add abstraction?
    }

    /**
     * Registers a player to the instance. If the player already exists, their
     * player record is updated, and if they are joining a game, they join it.
     * If the player does not exist, a new player instance is created and added to the instance.
     * This method is only to be used by entities actually retrieving login information.
     *
     * @param record       the player record containing the details of the player to be registered
     * @param registerData additional data that is to be used upon the first join of the player
     */
    public void registerPlayer(PlayerRecord record, InstancePlayer.RegisterData registerData) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(registerData, "registerData must not be null");
        getInstancePlayer(record.uuid()).ifPresentOrElse(player -> {
            player.applyRecord(record);
            player.setRegisterData(registerData);
            registerData.getGameId().flatMap(instance::getInstanceGame).ifPresent(game -> game.addReserved(player.getUuid()));
            instance.runOnMainThread(() -> joinInstance(record.uuid()));
        }, () -> {
            reserved.put(record.uuid(), new InstancePlayer(record, registerData));
            Optional.ofNullable(record.gameId()).flatMap(instance::getInstanceGame).ifPresent(game -> game.addReserved(record.uuid()));
        }); // TODO What happens if the player fails the connect?
    }

    /**
     * Add the player to the lists of the given queue type.
     * Also removes them from the lists that it does not belong to.
     * This does it on both the instance and the game.
     *
     * @param uuid      the player
     * @param game      the potential game to add the player to
     * @param queueType the queue type
     */
    private void applyQueueType(UUID uuid, InstanceGame game, QueueType queueType) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(queueType, "queueType must not be null");
        switch (queueType) {
            case ONLINE -> {
                online.add(uuid);
                players.remove(uuid);
                playing.remove(uuid);
            }
            case PLAYERS -> {
                online.add(uuid);
                players.add(uuid);
                playing.remove(uuid);
            }
            case PLAYING -> {
                online.add(uuid);
                players.add(uuid);
                playing.add(uuid);
            }
        }
        if (game != null) game.applyQueueType(uuid, queueType);
        sendInstanceStatus.run();
    }

    /**
     * This method is to be called when a player joins the instance.
     * This will transfer the player to the correct game, if applicable.
     *
     * @param uuid the player's uuid
     */
    public void joinInstance(UUID uuid) {
        getInstancePlayer(uuid).ifPresentOrElse(player -> {
            // Okay, we should allow the join, as it has been reserved
            Optional<InstanceGame> game = player.getRegisterData().getGameId().flatMap(instance::getInstanceGame);
            // When game is present, then the player tries to join a game, otherwise this instance.
            // First check whether if it has a reservation
            if (!containsReserved(uuid) || (game.isPresent() && !game.get().containsReserved(uuid))) {
                disconnectPlayer(uuid, Component.text("Got no reservation")); // TODO Translate
                return;
            }
            // We have a reservation, so we can proceed, first get the current queue types and then send the queue finished packet
            QueueType queueType = player.getRegisterData().queueType();
            QueueRequestParameter parameter = player.getRegisterData().parameter();
            boolean instanceSwitched = player.getInstanceId().map(id -> !id.equals(instance.getId())).orElse(true);
            boolean gameSwitched = player.getGame().map(obj -> player.getRegisterData().getGameId().map(id -> obj.getGameId() != id).orElse(true)).orElse(true);
            Optional<InstanceGame> oldGame = player.getGame();
            InstancePlayerListType oldInstanceListType = getInstancePlayerListType(player.getUuid());
            InstancePlayerListType oldGameListType = player.getGame().map(obj -> obj.getGamePlayerListType(player.getUuid())).orElse(InstancePlayerListType.NONE);
            InstancePlayerListType newListType = InstancePlayerListType.fromQueueType(queueType);
            try {
                instance.getConnection().<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, game.map(InstanceGame::getGameId).orElse(null)), null).getFutureResult().get();
                // Now apply the queue type
                applyQueueType(uuid, game.orElse(null), queueType);
                // First check if we are joining a game or not
                if (game.isPresent()) {
                    // We try to join a game
                    if (!instanceSwitched) {
                        // We were already on this instance
                        if (oldGame.isPresent() && gameSwitched) {
                            // We switched game, but we were already playing on one. Quit first
                            oldGame.get().onQuit(player);
                            oldGame.get().removeReserved(uuid);
                            instance.handleInstanceEvent(new InstanceQuitGameEvent(player, oldGame.get()));
                        }
                        if (oldInstanceListType != newListType) {
                            // Okay so we switched queue type of the instance
                            instance.handleInstanceEvent(new InstanceSwitchQueueTypeEvent(player, oldInstanceListType, queueType));
                        }
                        if (!gameSwitched) {
                            // We were already on the same game
                            if (oldGameListType != newListType) {
                                // Okay so we switched queue type of the game
                                game.get().onSwitchQueueType(player, oldGameListType, queueType, parameter);
                                instance.handleInstanceEvent(new InstanceSwitchGameQueueTypeEvent(player, game.get(), oldGameListType, queueType));
                                return;
                            }
                            // Same game, same queue type, we are done
                            return;
                        }
                        // We join a different game
                        game.get().onJoin(player, queueType, parameter);
                        instance.handleInstanceEvent(new InstanceJoinGameEvent(player, game.get(), queueType));
                    } else {
                        // We were not yet on this instance
                        instance.handleInstanceEvent(new InstanceJoinEvent(player, queueType));
                        game.get().onJoin(player, queueType, parameter);
                        instance.handleInstanceEvent(new InstanceJoinGameEvent(player, game.get(), queueType));
                    }
                } else {
                    // We try to join the instance only, check whether we just joined new
                    if (!instanceSwitched) {
                        // Ahh, so we were already on here
                        // If we were in a game, go out of it
                        if (oldGame.isPresent()) {
                            oldGame.get().onQuit(player);
                            oldGame.get().removeReserved(uuid);
                            instance.handleInstanceEvent(new InstanceQuitGameEvent(player, oldGame.get()));
                        }
                        if (oldInstanceListType != newListType) {
                            // Different queue type
                            instance.handleInstanceEvent(new InstanceSwitchQueueTypeEvent(player, oldInstanceListType, queueType));
                            return;
                        }
                        // Same instance, same queue type, we are done
                        return;
                    }
                    // We switched, normal join
                    instance.handleInstanceEvent(new InstanceJoinEvent(player, queueType));
                }
            } catch (UnsuccessfulResultException e) {
                disconnectPlayer(uuid, Component.text("Queue not finished")); // TODO Translate
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                disconnectPlayer(uuid, Component.text("Could not process queue")); // TODO Translate
            }
        }, () -> {
            // We do not have the details about this player. Controller did not send it.
            // Disconnect player, as we are unaware if this is correct.
            disconnectPlayer(uuid, Component.text("Incorrect state.")); // TODO Translateable
        });
    }

    /**
     * Sets that the given player is leaving the current server, only works for players on this instance.
     * If the player is on this server, will remove its data and call the correct functions.
     *
     * @param uuid
     */
    public void quitInstance(UUID uuid) {
        getInstancePlayer(uuid).ifPresent(player -> {
            quitGame(uuid);
            instance.handleInstanceEvent(new InstanceQuitEvent(player));
        });
        reserved.remove(uuid);
        online.remove(uuid);
        players.remove(uuid);
        playing.remove(uuid);
        sendInstanceStatus.run();
    }

    /**
     * Sets that the given player should quit its game, only works for players in this instance.
     *
     * @param uuid the player's uuid
     * @return a {@link CompletableFuture} that completes once the player has been removed from the game, or completes exceptionally if an error occurs.
     */
    public CompletableFuture<Void> quitGame(UUID uuid) {
        Optional<InstancePlayer> playerOpt = getInstancePlayer(uuid);
        if(playerOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalArgumentException("Player does not exist"));
        InstancePlayer player = playerOpt.get();
        Optional<InstanceGame> gameOpt = player.getGame();
        if(gameOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalArgumentException("Player is not in a game"));
        InstanceGame game = gameOpt.get();
        game.onQuit(player);
        game.removeReserved(uuid);
        instance.handleInstanceEvent(new InstanceQuitGameEvent(player, game));
        return instance.getConnection().<Void>sendRequestPacket(id -> new GameQuitPacket(id, uuid), null).getResult(); // TODO What if failed??
    }

    /**
     * Retrieves the saved locale associated with a network profile.
     * If no locale is found, the optional in the returned CompletableFuture will be empty.
     *
     * @param networkProfile the network profile for whom the saved locale is to be fetched; must not be null.
     * @return a {@link CompletableFuture} containing an optional with the profile's saved locale if available, otherwise an empty optional.
     */
    public CompletableFuture<Optional<Locale>> getSavedLocale(InstanceProfileData networkProfile) {
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        if(networkProfile.getType() != ProfileType.NETWORK) throw new IllegalArgumentException("networkProfile must be a network profile");
        return instance.getConnection().<String>sendRequestPacket(id -> new SavedLocalePacket.Get(id, networkProfile.getId()), null)
                .getResult().handle((data, ex) -> {
                   if(ex != null) return Optional.empty(); // TODO This is not really good to do!
                   return Optional.ofNullable(data).map(Locale::forLanguageTag);
                });
    }

    /**
     * Updates the saved locale for a given network profile in the data system with the provided locale.
     *
     * @param networkProfile the network profile. Must not be null.
     * @param locale the new locale to be saved for the network profile. Must not be null.
     * @return a {@link CompletableFuture} that completes once the locale is successfully saved, or completes exceptionally if an error occurs.
     */
    public CompletableFuture<Void> setSavedLocale(InstanceProfileData networkProfile, Locale locale) {
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        Objects.requireNonNull(locale, "locale cannot be null");
        if(networkProfile.getType() != ProfileType.NETWORK) throw new IllegalArgumentException("networkProfile must be a network profile");
        return instance.getConnection().<Void>sendRequestPacket(id -> new SavedLocalePacket.Set(id, networkProfile.getId(), locale.toLanguageTag()), null).getResult();
    }

    @Override
    public HashSet<UUID> getReserved() {
        return new HashSet<>(Set.copyOf(reserved.keySet()));
    }

    @Override
    public HashSet<UUID> getOnline() {
        return new HashSet<>(Set.copyOf(online));
    }

    @Override
    public HashSet<UUID> getPlayers() {
        return new HashSet<>(Set.copyOf(players));
    }

    @Override
    public HashSet<UUID> getPlaying() {
        return new HashSet<>(Set.copyOf(playing));
    }

    @Override
    public boolean isOnlineJoinable() {
        return onlineJoinable;
    }

    public void setOnlineJoinable(boolean onlineJoinable) {
        this.onlineJoinable = onlineJoinable;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isPlayersJoinable() {
        return playersJoinable;
    }

    public void setPlayersJoinable(boolean playersJoinable) {
        this.playersJoinable = playersJoinable;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isPlayingJoinable() {
        return playingJoinable;
    }

    public void setPlayingJoinable(boolean playingJoinable) {
        this.playingJoinable = playingJoinable;
        sendInstanceStatus.run();
    }

    @Override
    public int getMaxOnlineSlots() {
        return maxOnlineSlots;
    }

    public void setMaxOnlineSlots(int maxOnlineSlots) {
        this.maxOnlineSlots = maxOnlineSlots;
        sendInstanceStatus.run();
    }

    @Override
    public int getMaxPlayersSlots() {
        return maxPlayersSlots;
    }

    public void setMaxPlayersSlots(int maxPlayersSlots) {
        this.maxPlayersSlots = maxPlayersSlots;
        sendInstanceStatus.run();
    }

    @Override
    public int getMaxPlayingSlots() {
        return maxPlayingSlots;
    }

    public void setMaxPlayingSlots(int maxPlayingSlots) {
        this.maxPlayingSlots = maxPlayingSlots;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isOnlineKickable() {
        return onlineKickable;
    }

    public void setOnlineKickable(boolean onlineKickable) {
        this.onlineKickable = onlineKickable;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isPlayersKickable() {
        return playersKickable;
    }

    public void setPlayersKickable(boolean playersKickable) {
        this.playersKickable = playersKickable;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isPlayingKickable() {
        return playingKickable;
    }

    public void setPlayingKickable(boolean playingKickable) {
        this.playingKickable = playingKickable;
        sendInstanceStatus.run();
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        sendInstanceStatus.run();
    }

    @Override
    public boolean containsReserved(UUID uuid) {
        return reserved.containsKey(uuid);
    }

    @Override
    public boolean containsOnline(UUID uuid) {
        return online.contains(uuid);
    }

    @Override
    public boolean containsPlayers(UUID uuid) {
        return players.contains(uuid);
    }

    @Override
    public boolean containsPlaying(UUID uuid) {
        return playing.contains(uuid);
    }

    public void updateJoinableSlots(boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots, boolean onlineKickable, boolean playersKickable, boolean playingKickable, boolean isPrivate) {
        this.onlineJoinable = onlineJoinable;
        this.playersJoinable = playersJoinable;
        this.playingJoinable = playingJoinable;
        this.maxOnlineSlots = maxOnlineSlots;
        this.maxPlayersSlots = maxPlayersSlots;
        this.maxPlayingSlots = maxPlayingSlots;
        this.onlineKickable = onlineKickable;
        this.playersKickable = playersKickable;
        this.playingKickable = playingKickable;
        this.isPrivate = isPrivate;
        sendInstanceStatus.run();
    }

    /**
     * Retrieves the player list type of the player with the given UUID on the instance.
     *
     * @param player the player
     * @return the player list type, {@link InstancePlayerListType#NONE} if not in a list
     */
    public InstancePlayerListType getInstancePlayerListType(UUID player) {
        if (playing.contains(player)) return InstancePlayerListType.PLAYING;
        if (players.contains(player)) return InstancePlayerListType.PLAYERS;
        if (online.contains(player)) return InstancePlayerListType.ONLINE;
        if (reserved.contains(player)) return InstancePlayerListType.RESERVED;
        return InstancePlayerListType.NONE;
    }

    public void broadcast(Consumer<InstancePlayer> consumer) {
        getInstancePlayers().forEach(consumer);
    }

    // TODO Set locale in Paper.
    // TODO Set locale in Velocity
    // TODO Listen for VelocityClientSettingsChange
    // TODO Listen also in Paper??!
}
