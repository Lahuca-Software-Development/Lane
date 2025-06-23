package com.lahuca.laneinstance;

import com.lahuca.lane.connection.packet.QueueFinishedPacket;
import com.lahuca.lane.connection.packet.RequestInformationPacket;
import com.lahuca.lane.connection.packet.data.SavedLocalePacket;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.ResultUnsuccessfulException;
import com.lahuca.lane.game.Slottable;
import com.lahuca.lane.records.PlayerRecord;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class InstancePlayerManager implements Slottable {

    private final LaneInstance instance;
    private final Runnable sendInstanceStatus;

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

    InstancePlayerManager(LaneInstance instance, Runnable sendInstanceStatus, boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots) {
        this.instance = instance;
        this.sendInstanceStatus = sendInstanceStatus;
        this.onlineJoinable = onlineJoinable;
        this.playersJoinable = playersJoinable;
        this.playingJoinable = playingJoinable;
        this.maxOnlineSlots = maxOnlineSlots;
        this.maxPlayersSlots = maxPlayersSlots;
        this.maxPlayingSlots = maxPlayingSlots;
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
     * Retrieves a request of the player record of the player with the given UUID on the controller.
     * The value is null when no player with the given UUID is present.
     *
     * @param uuid the UUID of the player
     * @return the request
     */
    public Request<PlayerRecord> getPlayerRecord(UUID uuid) {
        if (uuid == null) return new Request<>(new ResultUnsuccessfulException(ResponsePacket.INVALID_PARAMETERS));
        return instance.getConnection().sendRequestPacket(id -> new RequestInformationPacket.Player(id, uuid), null);
    }

    /**
     * Retrieves a request of a collection of immutable records of all players on the controller.
     *
     * @return the request
     */
    public Request<ArrayList<PlayerRecord>> getAllPlayerRecords() {
        return instance.getConnection().sendRequestPacket(RequestInformationPacket.Players::new, null);
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
     * @param record the player record containing the details of the player to be registered
     * @param registerData additional data that is to be used upon the first join of the player
     */
    public void registerPlayer(PlayerRecord record, InstancePlayer.RegisterData registerData) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(registerData, "registerData must not be null");
        getInstancePlayer(record.uuid()).ifPresentOrElse(player -> {
            player.applyRecord(record);
            player.setRegisterData(registerData);
            player.getGameId().flatMap(instance::getInstanceGame).ifPresent(game -> game.addReserved(player.getUuid()));
            joinInstance(record.uuid());
        }, () -> {
            reserved.put(record.uuid(), new InstancePlayer(record, registerData));
            Optional.ofNullable(record.gameId()).flatMap(instance::getInstanceGame).ifPresent(game -> game.addReserved(record.uuid()));
        }); // TODO What happens if the player fails the connect?
    }

    /**
     * This method is to be called when a player joins the instance.
     * This will transfer the player to the correct game, if applicable.
     *
     * @param uuid the player's uuid
     */
    public void joinInstance(UUID uuid) {
        // TODO When we disconnect, maybe not always display message? Where would it be put? Chat?
        getInstancePlayer(uuid).ifPresentOrElse(player -> {
            // Okay, we should allow the join, as it has been reserved
            player.getGameId().ifPresentOrElse(gameId -> {
                // Player tries to join game
                instance.getInstanceGame(gameId).ifPresentOrElse(game -> {
                    // Player tries to join this instance, as it got a reservation, we assume it has a spot
                    // Send queue finished to controller
                    try {
                        instance.getConnection().<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, gameId), null).getFutureResult().get();
                        switch (player.getRegisterData().queueType()) {
                            case ONLINE -> {
                                online.add(player.getUuid());
                                game.addOnline(player.getUuid());
                            }
                            case PLAYER -> {
                                online.add(player.getUuid());
                                players.add(player.getUuid());
                                game.addPlayer(player.getUuid());
                            }
                            case PLAYING -> {
                                online.add(player.getUuid());
                                players.add(player.getUuid());
                                playing.add(player.getUuid());
                                game.addPlaying(player.getUuid());
                            }
                        }
                        sendInstanceStatus.run();
                        game.onJoin(player, player.getRegisterData().queueType());
                    } catch (ResultUnsuccessfulException e) {
                        disconnectPlayer(uuid, Component.text("Queue not finished")); // TODO Translate
                    } catch (InterruptedException | ExecutionException | CancellationException e) {
                        disconnectPlayer(uuid, Component.text("Could not process queue")); // TODO Translate
                    }
                }, () -> {
                    // The given game ID does not exist on this instance. Disconnect
                    disconnectPlayer(uuid, Component.text("Invalid game ID on instance.")); // TODO Translateable
                });
            }, () -> {
                // Player tries to join this instance, as it got a reservation, we assume it has a spot
                // Join allowed, finalize
                try {
                    instance.getConnection().<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, null), null).getFutureResult().get();
                    switch (player.getRegisterData().queueType()) {
                        case ONLINE -> online.add(player.getUuid());
                        case PLAYER -> {
                            online.add(player.getUuid());
                            players.add(player.getUuid());
                        }
                        case PLAYING -> {
                            online.add(player.getUuid());
                            players.add(player.getUuid());
                            playing.add(player.getUuid());
                        }
                    }
                    sendInstanceStatus.run();
                    instance.onInstanceJoin(player);
                } catch (ResultUnsuccessfulException e) {
                    disconnectPlayer(uuid, Component.text("Queue not finished")); // TODO Translate
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    disconnectPlayer(uuid, Component.text("Could not process queue")); // TODO Translate
                }
            });
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
        getInstancePlayer(uuid).ifPresent(player -> quitGame(uuid));
        reserved.remove(uuid);
        online.remove(uuid);
        players.remove(uuid);
        playing.remove(uuid);
        sendInstanceStatus.run();
    }

    /**
     * Sets that the given player is quitting its game, only works for players on this instance.
     *
     * @param uuid the player's uuid
     */
    public void quitGame(UUID uuid) {
        getInstancePlayer(uuid).ifPresent(player -> player.getGameId().flatMap(instance::getInstanceGame).ifPresent(game -> {
            game.onQuit(player);
            game.removeReserved(uuid);
        }));
    }

    /**
     * Retrieves the saved locale for a player with the given UUID.
     *
     * @param uuid the UUID of the player whose saved locale is being requested
     * @return a request object containing an optional Locale if successful, or empty if no locale is found or an error occurs
     */
    public Request<Optional<Locale>> getSavedLocale(UUID uuid) {
        if (uuid == null) return new Request<>(new ResultUnsuccessfulException(ResponsePacket.ILLEGAL_ARGUMENT));
        return instance.getConnection().<String>sendRequestPacket(id -> new SavedLocalePacket.Get(id, uuid), null).thenApplyConstruct((Function<ResultUnsuccessfulException, Optional<Locale>>) failed -> Optional.empty(), data -> data == null ? Optional.empty() : Optional.of(Locale.of(data)));
    }

    /**
     * Sets the saved locale for a player with the specified UUID.
     * This method sends a request to update the player's saved locale to the provided language.
     * If the UUID or Locale is null, an error response is returned.
     *
     * @param uuid   the UUID of the player whose saved locale is being set
     * @param locale the Locale to be saved for the player
     * @return a Request object representing the result of the operation;
     * contains an error if the arguments are invalid
     */
    public Request<Void> setSavedLocale(UUID uuid, Locale locale) {
        if (uuid == null || locale == null)
            return new Request<>(new ResultUnsuccessfulException(ResponsePacket.ILLEGAL_ARGUMENT));
        return instance.getConnection().sendRequestPacket(id -> new SavedLocalePacket.Set(id, uuid, locale.toLanguageTag()), null);
    }

    @Override
    public HashSet<UUID> getReserved() {
        return new HashSet<>(reserved.keySet());
    }

    @Override
    public HashSet<UUID> getOnline() {
        return new HashSet<>(online);
    }

    @Override
    public HashSet<UUID> getPlayers() {
        return new HashSet<>(players);
    }

    @Override
    public HashSet<UUID> getPlaying() {
        return new HashSet<>(playing);
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

    public void updateJoinableSlots(boolean onlineJoinable, boolean playersJoinable, boolean playingJoinable, int maxOnlineSlots, int maxPlayersSlots, int maxPlayingSlots) {
        this.onlineJoinable = onlineJoinable;
        this.playersJoinable = playersJoinable;
        this.playingJoinable = playingJoinable;
        this.maxOnlineSlots = maxOnlineSlots;
        this.maxPlayersSlots = maxPlayersSlots;
        this.maxPlayingSlots = maxPlayingSlots;
        sendInstanceStatus.run();
    }

    // TODO Set locale in Paper.
    // TODO Set locale in Velocity
    // TODO Listen for VelocityClientSettingsChange
    // TODO Listen also in Paper??!
}
