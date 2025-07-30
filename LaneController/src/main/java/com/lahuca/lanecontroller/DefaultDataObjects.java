package com.lahuca.lanecontroller;

import com.lahuca.lane.data.*;
import com.lahuca.lane.data.manager.DataManager;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This class contains helper functions for the default objects that are present in Lane.
 * These are solely to be used by the internal controller.
 */
class DefaultDataObjects {

    private static DataObjectId getPlayersUsernameId(UUID player) {
        return new DataObjectId(RelationalId.Players(player), "username");
    }

    /**
     * Gets the username from a player.
     * @param dataManager the data manager
     * @param player the player's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the username if it is present
     */
    static CompletableFuture<Optional<String>> getPlayersUsername(DataManager dataManager, UUID player) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getPlayersUsernameId(player))
                .thenApply(opt -> opt.flatMap(DataObject::getValue));
    }

    /**
     * Sets the username of a player.
     * @param dataManager the data manager
     * @param player the player's UUID
     * @param username the username
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setPlayersUsername(DataManager dataManager, UUID player, String username) {
        DataObject object = new DataObject(getPlayersUsernameId(player), PermissionKey.CONTROLLER, DataObjectType.STRING, username);
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getPlayersProfileId(UUID player) {
        return new DataObjectId(RelationalId.Players(player), "networkProfile");
    }

    /**
     * Gets the network profile from a player.
     * @param dataManager the data manager
     * @param player the player's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the network profile if it is present
     */
    static CompletableFuture<Optional<UUID>> getPlayersNetworkProfile(DataManager dataManager, UUID player) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getPlayersProfileId(player))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(UUID::fromString));
    }

    /**
     * Sets the network profile of a player.
     * @param dataManager the data manager
     * @param player the player's UUID
     * @param profile the profile's UUID
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setPlayersNetworkProfile(DataManager dataManager, UUID player, UUID profile) {
        DataObject object = new DataObject(getPlayersProfileId(player), PermissionKey.CONTROLLER, DataObjectType.STRING, profile.toString());
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getNetworkProfilesLocaleId(UUID profile) {
        return new DataObjectId(RelationalId.Profiles(profile), "locale");
    }

    /**
     * Gets the locale from a network profile.
     * @param dataManager the data manager
     * @param profile the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the network profile if it is present
     */
    static CompletableFuture<Optional<Locale>> getNetworkProfilesLocale(DataManager dataManager, UUID profile) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getNetworkProfilesLocaleId(profile))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(Locale::forLanguageTag));
    }

    /**
     * Sets the locale for a network profile.
     * @param dataManager the data manager
     * @param profile the profile's UUID
     * @param locale the locale
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setNetworkProfilesLocale(DataManager dataManager, UUID profile, Locale locale) {
        DataObject object = new DataObject(getNetworkProfilesLocaleId(profile), PermissionKey.CONTROLLER, DataObjectType.STRING, locale.toLanguageTag());
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getUsernamesUuidId(String username) {
        return new DataObjectId(RelationalId.Usernames(username), "uuid");
    }

    /**
     * Gets the UUID from a username.
     * @param dataManager the data manager
     * @param username the username
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the UUID if it is present
     */
    static CompletableFuture<Optional<UUID>> getUsernamesUuid(DataManager dataManager, String username) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getUsernamesUuidId(username))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(UUID::fromString));
    }

    /**
     * Sets the UUID for a username.
     * @param dataManager the data manager
     * @param username the username
     * @param uuid the UUID
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setUsernamesUuid(DataManager dataManager, String username, UUID uuid) {
        DataObject object = new DataObject(getUsernamesUuidId(username), PermissionKey.CONTROLLER, DataObjectType.STRING, uuid.toString());
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

}
