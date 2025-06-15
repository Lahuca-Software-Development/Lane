package com.lahuca.lanecontroller;

import com.lahuca.lane.data.DataObject;
import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.PermissionKey;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.manager.DataManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class ControllerPlayerManager {

    private final Controller controller;
    private final DataManager dataManager;

    private final ConcurrentHashMap<UUID, ControllerPlayer> players = new ConcurrentHashMap<>();

    public ControllerPlayerManager(Controller controller, DataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }

    void put(UUID uuid, ControllerPlayer player) {
        players.put(uuid, player); // TODO ????
    }

    // TODO Redo
    public void leavePlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        players.remove(controllerPlayer.getUuid());
    }

    /**
     * Registers a player in the system with the provided information
     * Also updates the data object so that the username is stored to the UUID.
     * Returns the effective locale to the saved locale, if it is not present, it will be set to the default locale.
     *
     * @param uuid the player's uuid
     * @param username the player's username
     * @param defaultLocale the default locale to be set for the player if no saved locale is found. Must not be null.
     * @return {@code null} if the UUID is already registered, otherwise the effective locale to set.
     * @throws IllegalArgumentException when any of the arguments is null
     */
    public Locale registerPlayer(UUID uuid, String username, Locale defaultLocale) {
        if (uuid == null || username == null || defaultLocale == null) {
            throw new IllegalArgumentException("player, username and defaultLocale cannot be null");
        }
        ControllerPlayer player = new ControllerPlayer(uuid, username, username);
        if (players.containsKey(player.getUuid())) return null;
        players.put(player.getUuid(), player);
        dataManager.writeDataObject(PermissionKey.CONTROLLER, new DataObject(new DataObjectId(RelationalId.Players(player.getUuid()), "username"), PermissionKey.CONTROLLER, player.getUsername()));
        applySavedLocale(player.getUuid(), defaultLocale);
        try {
            // TODO This is blocking, but doing it using whenComplete; is incorrect in the register. As it will not find the player yet
            Optional<DataObject> object = dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(player.getUuid()), "locale")).get();
            if (object != null && object.isPresent()) {
                return object.flatMap(DataObject::getValue).map(Locale::forLanguageTag).orElse(defaultLocale);
            }
            return defaultLocale;
        } catch (InterruptedException | ExecutionException e) {
            return defaultLocale;
        }
    }

    /**
     * Applies the effective locale for a player.
     * Fetches the player's saved locale from the data manager.
     * If a saved locale is found, it is set as the player's effective locale.
     * If no saved locale is available, the default locale is applied instead.
     *
     * @param player        the unique identifier of the player whose locale is to be set. Must not be null.
     * @param defaultLocale the default locale to apply if no saved locale is found. Must not be null.
     */
    public void applySavedLocale(UUID player, Locale defaultLocale) {
        dataManager.readDataObject(PermissionKey.CONTROLLER, new DataObjectId(RelationalId.Players(player), "locale")).whenComplete((object, exception) -> {
            // We tried to fetch the saved locale. If present set to saved locale, otherwise client locale.
            if (object != null && object.isPresent()) {
                object.flatMap(DataObject::getValue).ifPresentOrElse(savedLocale -> controller.setEffectiveLocale(player, Locale.of(savedLocale)),
                        () -> controller.setEffectiveLocale(player, defaultLocale));
            } else {
                controller.setEffectiveLocale(player, defaultLocale);
            }
        });
    }

    public void unregisterPlayer(UUID player) {
        players.remove(player);
        // TODO Remove from party, disband party, etc.?
        //  or maybe keep it, but then hmm
    } // TODO Redo

    public @NotNull Collection<ControllerPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid)); // TODO Maybe add this as short cut into Controller? So it is not controller.getPlayerManager().getPlayer(uuid) but controller.getPlayer(uuid)
    } // TODO Redo

    public Optional<ControllerPlayer> getPlayerByUsername(String name, boolean caseInsensitive) { // TODO Redo
        return players.values().stream().filter(player -> (caseInsensitive && player.getUsername().equalsIgnoreCase(name))
                || (!caseInsensitive && player.getUsername().equals(name))).findFirst();
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
        // TODO Case insentivive? Then also edit the boolean on the next line: false to true/var
        // usernames.Laurenshup.uuid = UUID
        Optional<UUID> optional = getPlayerByUsername(username, false).map(ControllerPlayer::getUuid);
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
        // TODO Cache?
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
