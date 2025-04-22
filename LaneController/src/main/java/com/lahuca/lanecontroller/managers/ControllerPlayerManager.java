package com.lahuca.lanecontroller.managers;

import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerGame;
import com.lahuca.lanecontroller.ControllerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerPlayerManager {

    private final Controller controller;
    private final DataManager dataManager;

    private final ConcurrentHashMap<UUID, ControllerPlayer> players = new ConcurrentHashMap<>();

    public ControllerPlayerManager(Controller controller, DataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }

    void put(UUID uuid, ControllerPlayer player) {
        players.put(uuid, player);
    }

    // TODO Redo
    public void leavePlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        players.remove(controllerPlayer.getUuid());
    }

    /**
     * Registers a player in the system with the provided information.
     * Sets the effective locale to the saved locale, if it is not present, it will be set to the default locale.
     * Also updates the data object so that the username is stored to the UUID.
     *
     * @param uuid          the unique identifier for the player. Must not be null.
     * @param username      the username of the player. Must not be null.
     * @param defaultLocale the default locale to be set for the player if no saved locale is found. Must not be null.
     * @return {@code true} if the player was successfully registered, {@code false} if the UUID is already registered or any parameter is null.
     */
    public boolean registerPlayer(UUID uuid, String username, Locale defaultLocale) {
        if (uuid == null || username == null || defaultLocale == null) {
            return false;
        }
        if (players.containsKey(uuid)) return false;
        ControllerPlayer player = new ControllerPlayer(uuid, username, username); // TODO Display name!
        players.put(uuid, player);
        dataManager.writeDataObject(PermissionKey.CONTROLLER, new DataObject(new DataObjectId(RelationalId.Players(player.getUuid()), "username"), PermissionKey.CONTROLLER, player.getUsername()));
        dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(player.getUuid()), "locale")).whenComplete((object, exception) -> {
            // We tried to fetch the saved locale. If present set to saved locale, otherwise client locale.
            if (object != null && object.isPresent()) {
                object.flatMap(DataObject::getValue).ifPresentOrElse(savedLocale -> controller.setEffectiveLocale(uuid, Locale.of(savedLocale)),
                        () -> controller.setEffectiveLocale(uuid, defaultLocale));
            } else {
                controller.setEffectiveLocale(uuid, defaultLocale);
            }
        });
        return true;
    }

    public void unregisterPlayer(UUID player) {
        players.remove(player);
    } // TODO Redo

    public @NotNull Collection<ControllerPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid)); // TODO Maybe add this as short cut into Controller? So it is not controller.getPlayerManager().getPlayer(uuid) but controller.getPlayer(uuid)
    } // TODO Redo

    public Optional<ControllerPlayer> getPlayerByUsername(String name) { // TODO Redo
        return players.values().stream().filter(player -> player.getUsername().equals(name)).findFirst();
    }

    /**
     * Gets the last known username of the player with the given UUID.
     * It is taken either immediately if the player is online, otherwise the value that is present in the data manager.
     *
     * @param uuid the player's UUID.
     * @return a CompletableFuture with an optional, if data has been found the optional is populated with the username; otherwise it is empty.
     */
    public CompletableFuture<Optional<String>> getPlayerUsername(UUID uuid) {
        // TODO Allow option to enable case-insensitive
        Optional<String> optional = getPlayer(uuid).map(ControllerPlayer::getUsername);
        if (optional.isPresent()) {
            return CompletableFuture.completedFuture(optional);
        }
        // TODO Probably we want to cache!
        // TODO Put in!
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(uuid), "username")).thenApply(dataObject ->
                dataObject.flatMap(DataObject::getValue));
    }

    /**
     * Gets the last known UUID of the player with the given username.
     * It is taken either immediately if the player is online, otherwise the value that is present in the data manager.
     *
     * @param username the player's username
     * @return a CompletableFuture with an optional, if data has been found the optional is populated with the UUID; otherwise it is empty
     */
    public CompletableFuture<Optional<UUID>> getPlayerUuid(String username) {
        // TODO
        // usernames.Laurenshup.uuid = UUID
        Optional<UUID> optional = getPlayerByUsername(username).map(ControllerPlayer::getUuid);
        if (optional.isPresent()) {
            return CompletableFuture.completedFuture(optional);
        }
        // TODO Probably we want to caache!
        // TODO Put in
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Usernames(username), "uuid"))
                .thenApply(dataObject -> dataObject.flatMap(object -> object.getValue().map(UUID::fromString)));
    }

    /**
     * Gets the last known username of the player with the given UUID.
     *
     * @param uuid the uuid
     * @return a completable future that retrieves an optional that has the username or is empty when no one with the given UUID was online at least once.
     */
    public CompletableFuture<Optional<String>> getOfflinePlayerName(UUID uuid) {
        // TODO Put the username in!
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(uuid), "username")).thenApply(dataObject -> {
            if (dataObject.isPresent()) {
                return dataObject.get().getValue();
            }
            return Optional.empty();
        });
    }

    /**
     * Retrieves the saved locale associated with a player based on their unique identifier (UUID).
     * If no locale is found, the optional in the returned CompletableFuture will be empty.
     *
     * @param uuid the unique identifier of the player for whom the saved locale is to be fetched; must not be null.
     * @return a CompletableFuture containing an optional with the player's saved locale if available, otherwise an empty optional.
     */
    public CompletableFuture<Optional<Locale>> getSavedLocale(UUID uuid) {
        if(uuid == null) return CompletableFuture.failedFuture(new IllegalArgumentException("UUID must not be null"));
        return dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(uuid), "locale")) // TODO maybe save some kind of constructor of this ID. Also for "username", as it is used alot.
                .thenApply(dataOptional -> dataOptional.flatMap(dataObject -> dataObject.getValue().map(Locale::of)));
    }

    /**
     * Updates the saved locale for a given player in the data system with the provided locale.
     * In case the player UUID or locale is null, the method will return a failed CompletableFuture.
     *
     * @param uuid   the unique identifier of the player. Must not be null.
     * @param locale the new locale to be saved for the player. Must not be null.
     * @return a CompletableFuture that completes once the locale is successfully saved, or completes exceptionally if an error occurs.
     */
    public CompletableFuture<Void> setSavedLocale(UUID uuid, Locale locale) {
        if(uuid == null || locale == null) return CompletableFuture.failedFuture(new IllegalArgumentException("UUID and locale must not be null"));
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, new DataObject(new DataObjectId(RelationalId.Players(uuid), "locale"), PermissionKey.CONTROLLER, locale.toLanguageTag()));
    }

}
