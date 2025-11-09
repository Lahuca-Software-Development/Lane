package com.lahuca.lanecontroller;

import com.google.gson.Gson;
import com.lahuca.lane.data.*;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.records.RelationshipRecord;

import java.util.*;
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
     *
     * @param dataManager the data manager
     * @param player      the player's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the username if it is present
     */
    static CompletableFuture<Optional<String>> getPlayersUsername(DataManager dataManager, UUID player) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getPlayersUsernameId(player))
                .thenApply(opt -> opt.flatMap(DataObject::getValue));
    }

    static CompletableFuture<Optional<String>> getNetworkProfilesUsername(DataManager dataManager, UUID networkProfile) {
        return Controller.getInstance().getDataManager().getProfileData(networkProfile)
                .thenCompose(profileOpt -> profileOpt
                        .map(profile -> getPlayersUsername(dataManager, profile.getFirstSuperProfile()))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    /**
     * Sets the username of a player.
     *
     * @param dataManager the data manager
     * @param player      the player's UUID
     * @param username    the username
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
     *
     * @param dataManager the data manager
     * @param player      the player's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the network profile if it is present
     */
    static CompletableFuture<Optional<UUID>> getPlayersNetworkProfile(DataManager dataManager, UUID player) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getPlayersProfileId(player))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(UUID::fromString));
    }

    /**
     * Sets the network profile of a player.
     *
     * @param dataManager the data manager
     * @param player      the player's UUID
     * @param profile     the profile's UUID
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
     *
     * @param dataManager the data manager
     * @param profile     the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the locale if it is present
     */
    static CompletableFuture<Optional<Locale>> getNetworkProfilesLocale(DataManager dataManager, UUID profile) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getNetworkProfilesLocaleId(profile))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(Locale::forLanguageTag));
    }

    /**
     * Sets the locale for a network profile.
     *
     * @param dataManager the data manager
     * @param profile     the profile's UUID
     * @param locale      the locale
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setNetworkProfilesLocale(DataManager dataManager, UUID profile, Locale locale) {
        DataObject object = new DataObject(getNetworkProfilesLocaleId(profile), PermissionKey.CONTROLLER, DataObjectType.STRING, locale.toLanguageTag());
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getNetworkProfilesNicknameId(UUID profile) {
        return new DataObjectId(RelationalId.Profiles(profile), "nickname");
    }

    /**
     * Gets the nickname from a network profile.
     *
     * @param dataManager the data manager
     * @param profile     the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the nickname if it is present
     */
    static CompletableFuture<Optional<String>> getNetworkProfilesNickname(DataManager dataManager, UUID profile) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getNetworkProfilesNicknameId(profile))
                .thenApply(opt -> opt.flatMap(DataObject::getValue));
    }

    /**
     * Sets the nickname for a network profile.
     *
     * @param dataManager the data manager
     * @param profile     the profile's UUID
     * @param nickname    the nickname
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setNetworkProfilesNickname(DataManager dataManager, UUID profile, String nickname) {
        if(nickname == null) {
            return dataManager.removeDataObject(PermissionKey.CONTROLLER, getNetworkProfilesNicknameId(profile));
        }
        DataObject object = new DataObject(getNetworkProfilesNicknameId(profile), PermissionKey.CONTROLLER, DataObjectType.STRING, nickname);
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getNetworkProfilesFriendsId(UUID profile) {
        return new DataObjectId(RelationalId.Profiles(profile), "friends");
    }

    /**
     * Gets the friends from a network profile.
     *
     * @param dataManager the data manager
     * @param gson        the gson instance to use when parsing
     * @param profile     the profile's UUID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the friendship IDs if it is present
     */
    static CompletableFuture<Optional<List<Long>>> getNetworkProfilesFriends(DataManager dataManager, Gson gson, UUID profile) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getNetworkProfilesFriendsId(profile))
                .thenApply(opt -> opt.flatMap(obj -> obj.getValueAsLongArray(gson)));
    }

    /**
     * Sets the friends of a network profile.
     *
     * @param dataManager   the data manager
     * @param profile       the profile's UUID
     * @param friendshipIds the friendship IDs
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setNetworkProfilesFriends(DataManager dataManager, UUID profile, List<Long> friendshipIds) {
        if(friendshipIds == null || friendshipIds.isEmpty()) {
            return dataManager.removeDataObject(PermissionKey.CONTROLLER, getNetworkProfilesFriendsId(profile));
        }
        DataObject object = new DataObject(getNetworkProfilesFriendsId(profile), PermissionKey.CONTROLLER, DataObjectType.ARRAY, friendshipIds);
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    /**
     * Adds a friendship ID to the friends of a network profile.
     *
     * @param dataManager  the data manager
     * @param gson         the gson to use when parsing
     * @param profile      the network profile's UUID
     * @param friendshipId the friendship ID to add
     * @return a {@link CompletableFuture} with a void to signify success: it has been added
     */
    static CompletableFuture<Void> addNetworkProfilesFriends(DataManager dataManager, Gson gson, UUID profile, long friendshipId) {
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, getNetworkProfilesFriendsId(profile), obj -> {
            List<Long> ids = obj.getValueAsLongArray(gson).orElseGet(ArrayList::new);
            ids.add(friendshipId);
            obj.setValue(gson, ids);
            return true;
        }).thenCompose(status -> {
            if(!status) {
                return dataManager.writeDataObject(PermissionKey.CONTROLLER,
                        new DataObject(getNetworkProfilesFriendsId(profile), PermissionKey.CONTROLLER, DataObjectType.ARRAY, List.of(friendshipId)));
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Remove a friendship ID from the friends of a network profile.
     *
     * @param dataManager  the data manager
     * @param gson         the gson to use when parsing
     * @param profile      the network profile's UUID
     * @param friendshipId the friendship ID to remove
     * @return a {@link CompletableFuture} with a void to signify success: it has been removed
     */
    static CompletableFuture<Void> removeNetworkProfilesFriends(DataManager dataManager, Gson gson, UUID profile, long friendshipId) {
        return dataManager.updateDataObject(PermissionKey.CONTROLLER, getNetworkProfilesFriendsId(profile), obj -> {
            List<Long> ids = obj.getValueAsLongArray(gson).orElseGet(ArrayList::new);
            ids.remove(friendshipId);
            obj.setValue(gson, ids);
            return true;
        }).thenAccept(status -> {
        });
    }

    private static DataObjectId getUsernamesUuidId(String username) {
        return new DataObjectId(RelationalId.Usernames(username), "uuid");
    }

    /**
     * Gets the UUID from a username.
     *
     * @param dataManager the data manager
     * @param username    the username
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the UUID if it is present
     */
    static CompletableFuture<Optional<UUID>> getUsernamesUuid(DataManager dataManager, String username) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getUsernamesUuidId(username))
                .thenApply(opt -> opt.flatMap(DataObject::getValue).map(UUID::fromString));
    }

    /**
     * Sets the UUID for a username.
     *
     * @param dataManager the data manager
     * @param username    the username
     * @param uuid        the UUID
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setUsernamesUuid(DataManager dataManager, String username, UUID uuid) {
        DataObject object = new DataObject(getUsernamesUuidId(username), PermissionKey.CONTROLLER, DataObjectType.STRING, uuid.toString());
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

    private static DataObjectId getFriendshipsDataId(long friendshipId) {
        return new DataObjectId(RelationalId.Friendships(friendshipId), "data");
    }

    /**
     * Gets the data from a friendship.
     *
     * @param dataManager  the data manager
     * @param gson         the gson instance to use when parsing
     * @param friendshipId the friendship ID
     * @return a {@link CompletableFuture} with a {@link Optional} whose value will contain the data if it is present
     */
    static CompletableFuture<Optional<RelationshipRecord>> getFriendshipsData(DataManager dataManager, Gson gson, long friendshipId) {
        return dataManager.readDataObject(PermissionKey.CONTROLLER, getFriendshipsDataId(friendshipId))
                .thenApply(opt -> opt.flatMap(obj -> obj.getValue(gson, RelationshipRecord.class)));
    }

    /**
     * Sets the data for a friendship.
     *
     * @param dataManager  the data manager
     * @param gson         the gson instance to use when parsing
     * @param friendshipId the friendship ID
     * @param data         the data
     * @return a {@link CompletableFuture} with a void to signify success: it has been updated
     */
    static CompletableFuture<Void> setFriendshipsData(DataManager dataManager, Gson gson, long friendshipId, RelationshipRecord data) {
        DataObject object = new DataObject(getFriendshipsDataId(friendshipId), PermissionKey.CONTROLLER, gson, data);
        return dataManager.writeDataObject(PermissionKey.CONTROLLER, object);
    }

}
