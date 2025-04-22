package com.lahuca.laneinstance.managers;

import com.lahuca.lane.connection.packet.QueueFinishedPacket;
import com.lahuca.lane.connection.packet.RequestInformationPacket;
import com.lahuca.lane.connection.packet.SendMessagePacket;
import com.lahuca.lane.connection.packet.data.SavedLocalePacket;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.LaneInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class InstancePlayerManager {

    private final LaneInstance instance;
    private final Runnable sendInstanceStatus;

    private final ConcurrentHashMap<UUID, InstancePlayer> players = new ConcurrentHashMap<>();

    public InstancePlayerManager(LaneInstance instance, Runnable sendInstanceStatus) {
        this.instance = instance;
        this.sendInstanceStatus = sendInstanceStatus;
    }

    /**
     * Retrieves an InstancePlayer by a given UUID on this instance.
     *
     * @param player the UUID of the player
     * @return an optional of the InstancePlayer object.
     */
    public Optional<InstancePlayer> getInstancePlayer(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    /**
     * Retrieves a collection of all instance players on this instance.
     *
     * @return the collection
     */
    public Collection<InstancePlayer> getInstancePlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    private static <T> Request<T> simpleRequest(String result) { // TODO Move
        return new Request<>(new Result<>(result));
    }

    /**
     * Retrieves a request of the player record of the player with the given UUID on the controller.
     * The value is null when no player with the given UUID is present.
     *
     * @param uuid the UUID of the player
     * @return the request
     */
    public Request<PlayerRecord> getPlayerRecord(UUID uuid) {
        if (uuid == null) return simpleRequest(ResponsePacket.INVALID_PARAMETERS);
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

    public void sendMessage(UUID player, Component component) {
        if (player == null || component == null) return;
        instance.getConnection().sendPacket(new SendMessagePacket(player, GsonComponentSerializer.gson().serialize(component)), null);
    }

    private void disconnectPlayer(UUID uuid, Component message) {
        instance.disconnectPlayer(uuid, message); // TODO Move to here? Maybe add abstraction?
    }

    /**
     * Registers a player to the instance. If the player already exists, their
     * player record is updated. If the player does not exist, a new player
     * instance is created and added to the instance.
     * This method is only to be used by entities actually retrieving login information.
     *
     * @param record the player record containing the details of the player to be registered
     */
    public void registerPlayer(PlayerRecord record) {
        getInstancePlayer(record.uuid()).ifPresentOrElse(player -> player.applyRecord(record),
                () -> players.put(record.uuid(), new InstancePlayer(record))); // TODO What happens if the player fails the connect?
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
            // Okay, we should allow the join.
            player.getGameId().ifPresentOrElse(gameId -> {
                // Player tries to join game
                instance.getInstanceGame(gameId).ifPresentOrElse(game -> {
                    // Player tries to join this instance. Check if possible.
                    if (!instance.isJoinable() || instance.getCurrentPlayers() >= instance.getMaxPlayers()) {
                        // We cannot be at this instance.
                        disconnectPlayer(uuid, Component.text("Instance not joinable or full")); // TODO Translate
                        return;
                    }
                    // TODO Maybe game also has slots? Or limitations?
                    // Send queue finished to controller
                    try {
                        Result<Void> result = instance.getConnection().<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, gameId), null).getFutureResult().get();
                        if (result == null || !result.isSuccessful()) {
                            disconnectPlayer(uuid, Component.text("Queue not finished")); // TODO Translate
                            return;
                        }
                        sendInstanceStatus.run();
                        game.onJoin(player);
                    } catch (InterruptedException | ExecutionException | CancellationException e) {
                        disconnectPlayer(uuid, Component.text("Could not process queue")); // TODO Translate
                    }
                }, () -> {
                    // The given game ID does not exist on this instance. Disconnect
                    disconnectPlayer(uuid, Component.text("Invalid game ID on instance.")); // TODO Translateable
                });
            }, () -> {
                // Player tries to join this instance. Check if possible.
                if (!instance.isJoinable() || instance.getCurrentPlayers() >= instance.getMaxPlayers()) {
                    // We cannot be at this instance.
                    disconnectPlayer(uuid, Component.text("Instance not joinable or full")); // TODO Translate
                    return;
                }
                // Join allowed, finalize
                try {
                    Result<Void> result = instance.getConnection().<Void>sendRequestPacket(id -> new QueueFinishedPacket(id, uuid, null), null).getFutureResult().get();
                    if (!result.isSuccessful()) {
                        disconnectPlayer(uuid, Component.text("Queue not finished")); // TODO Translate
                        return;
                    }
                    sendInstanceStatus.run();
                    // TODO Handle, like TP, etc. Only after response.
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
        players.remove(uuid);
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
            // TODO Remove player actually from player list in the game
        }));
    }

    /**
     * Retrieves the saved locale for a player with the given UUID.
     *
     * @param uuid the UUID of the player whose saved locale is being requested
     * @return a request object containing an optional Locale if successful, or empty if no locale is found or an error occurs
     */
    public Request<Optional<Locale>> getSavedLocale(UUID uuid) {
        if (uuid == null) return new Request<>(new Result<>(ResponsePacket.ILLEGAL_ARGUMENT));
        return instance.getConnection().<String>sendRequestPacket(id -> new SavedLocalePacket.Get(id, uuid), null)
                .thenApplyConstruct(result -> {
                    if(result.isSuccessful()) {
                        return new Result<>(result.result(), result.data() == null ? Optional.empty() : Optional.of(Locale.of(result.data())));
                    }
                    return new Result<>(result.result(), Optional.empty());
                });
    }

    /**
     * Sets the saved locale for a player with the specified UUID.
     * This method sends a request to update the player's saved locale to the provided language.
     * If the UUID or Locale is null, an error response is returned.
     *
     * @param uuid the UUID of the player whose saved locale is being set
     * @param locale the Locale to be saved for the player
     * @return a Request object representing the result of the operation;
     *         contains an error if the arguments are invalid
     */
    public Request<Void> setSavedLocale(UUID uuid, Locale locale) {
        if(uuid == null || locale == null) return new Request<>(new Result<>(ResponsePacket.ILLEGAL_ARGUMENT));
        return instance.getConnection().sendRequestPacket(id -> new SavedLocalePacket.Set(id, uuid, locale.toLanguageTag()), null);
    }

    // TODO Set locale in Paper.
    // TODO Set locale in Velocity
    // TODO Listen for VelocityClientSettingsChange
    // TODO Listen also in Paper??!
}
