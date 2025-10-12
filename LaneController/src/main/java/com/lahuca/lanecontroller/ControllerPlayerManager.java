package com.lahuca.lanecontroller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lane.data.profile.ProfileType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

public class ControllerPlayerManager {

    private final Controller controller;
    private final DataManager dataManager;

    private final ConcurrentHashMap<UUID, ControllerPlayer> players = new ConcurrentHashMap<>();
    private final Cache<UUID, Long> networkProcessing = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES) // TODO Change the time
            .removalListener((UUID uuid, Long addedTime, RemovalCause cause) -> getPlayer(uuid).ifPresent(player -> {
                if (!player.isNetworkProcessed() && cause.wasEvicted()) {
                    player.process(false, Component.translatable("lane.controller.error.login.processingTimeout"));
                }
            })).build();

    public ControllerPlayerManager(Controller controller, DataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }

    /**
     * Registers a player in the system with the provided information
     * Also updates the data object so that the username is stored to the UUID.
     * Returns the effective locale to the saved locale, if it is not present, it will be set to the default locale.
     *
     * @param uuid          the player's uuid
     * @param username      the player's username
     * @param defaultLocale the default locale to be set for the player if no saved locale is found. Must not be null.
     * @return {@code null} if the UUID is already registered, otherwise the effective locale to set.
     * @throws IllegalArgumentException when any of the arguments is null
     */
    public Locale registerPlayer(UUID uuid, String username, Locale defaultLocale) throws InterruptedException, ExecutionException {
        if (uuid == null || username == null || defaultLocale == null) {
            throw new IllegalArgumentException("player, username and defaultLocale cannot be null");
        }
        if (players.containsKey(uuid)) return null;
        // Get network profile
        CompletableFuture<UUID> networkProfileUuidFuture = DefaultDataObjects.getPlayersNetworkProfile(dataManager, uuid).thenCompose(opt -> {
            // If UUID is present, return it; otherwise create new profile
            return opt.<CompletionStage<UUID>>map(CompletableFuture::completedFuture)
                    .orElseGet(() -> controller.createNewProfile(ProfileType.NETWORK)
                            .thenCompose(profile -> {
                                // We created and fetched a profile, now set it in the data manager
                                return controller.setNewNetworkProfile(uuid, profile).thenApply(data -> profile.getId());
                            }));
            // We have a profile, return its value
        });
        UUID networkProfileUuid = networkProfileUuidFuture.get(); // TODO Blocking
        Optional<String> nickname = DefaultDataObjects.getNetworkProfilesNickname(dataManager, networkProfileUuid).get(); // TODO Blocking
        ControllerPlayer player = new ControllerPlayer(uuid, username, networkProfileUuid, nickname.orElse(null));
        if (players.containsKey(player.getUuid())) return null;
        players.put(player.getUuid(), player);
        // Store info: last used username, username to UUID
        DefaultDataObjects.setPlayersUsername(dataManager, player.getUuid(), username);
        DefaultDataObjects.setUsernamesUuid(dataManager, username, player.getUuid());
        applySavedLocale(player.getUuid(), networkProfileUuid, defaultLocale); // TODO Check result?
        controller.getPartyManager().getParties().forEach(party -> {
            if(party.containsPlayer(player)) player.setPartyId(party.getId());
        });
        try {
            // TODO This is blocking, but doing it using whenComplete; is incorrect in the register. As it will not find the player yet
            Optional<Locale> object = DefaultDataObjects.getNetworkProfilesLocale(dataManager, networkProfileUuid).get();
            return object.orElse(defaultLocale);
        } catch (InterruptedException | ExecutionException e) {
            return defaultLocale;
        }
    }

    /**
     * Requests plugins to mark whether they still need to do additional processing after a player has joined.
     *
     * @param player the player to do the network processing of
     * @throws IllegalArgumentException when the given player is null
     * @throws IllegalStateException    when the player is already processed
     */
    public void doNetworkProcessing(ControllerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        if (player.isNetworkProcessed()) throw new IllegalStateException("player is already processed");
        // Add to timeout cache and then run the processor with a succesful state
        networkProcessing.put(player.getUuid(), System.currentTimeMillis());
        player.process(true, null);
    }

    /**
     * Applies the effective locale for a player.
     * First fetches the network profile of the player.
     * Using the network profile, the saved locale of the network profile is retrieved.
     * If a saved locale is found, it is set as the player's effective locale.
     * Otherwise, the default locale is applied instead.
     *
     * @param player        the unique identifier of the player whose locale is to be set. Must not be null.
     * @param defaultLocale the default locale to apply if no saved locale is found. Must not be null.
     * @return a {@link CompletableFuture} with a void to signify success: it has been correctly applied
     * @see #applySavedLocale(UUID, UUID, Locale)
     */
    public CompletableFuture<Void> applySavedLocalePlayer(UUID player, Locale defaultLocale) {
        return DefaultDataObjects.getPlayersNetworkProfile(dataManager, player).thenCompose(opt -> {
            if (opt.isEmpty())
                return CompletableFuture.failedFuture(new IllegalStateException("No network profile found"));
            return applySavedLocale(player, opt.get(), defaultLocale);
        }); // TODO Maybe move the function to ControllerPlayer?
    }

    /**
     * Applies the effective locale for a player.
     * Fetches the network profile's saved locale of the player from the data manager.
     * If a saved locale is found, it is set as the player's effective locale.
     * If no saved locale is available, the default locale is applied instead.
     *
     * @param player         the unique identifier of the player whose locale is to be set. Must not be null.
     * @param networkProfile the unique identifier of the network profile to retrieve the saved locale from. Must not be null.
     * @param defaultLocale  the default locale to apply if no saved locale is found. Must not be null.
     * @return a {@link CompletableFuture} with a void to signify success: it has been correctly applied
     */
    public CompletableFuture<Void> applySavedLocale(UUID player, UUID networkProfile, Locale defaultLocale) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        Objects.requireNonNull(defaultLocale, "defaultLocale cannot be null");
        return DefaultDataObjects.getNetworkProfilesLocale(dataManager, networkProfile).thenAccept(locale ->
                controller.setEffectiveLocale(player, locale.orElse(defaultLocale)));// TODO Maybe move the function to ControllerPlayer?
    }

    public void unregisterPlayer(UUID player) {
        players.remove(player);
        networkProcessing.invalidate(player);
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
     * @return a {@link CompletableFuture} with an {@link Optional}, if data has been found the optional is populated with the username; otherwise it is empty.
     */
    public CompletableFuture<Optional<String>> getPlayerUsername(UUID uuid) {
        // TODO Allow option to enable case-insensitive
        Optional<String> optional = getPlayer(uuid).map(ControllerPlayer::getUsername);
        if (optional.isPresent()) {
            return CompletableFuture.completedFuture(optional);
        }
        // TODO Probably we want to cache!
        return DefaultDataObjects.getPlayersUsername(dataManager, uuid);
    }

    /**
     * Gets the last known UUID of the player with the given username.
     * It is taken either immediately if the player is online, otherwise the value that is present in the data manager.
     *
     * @param username the player's username
     * @return a {@link CompletableFuture} with an {@link Optional}, if data has been found, the optional is populated with the UUID; otherwise it is empty
     */
    public CompletableFuture<Optional<UUID>> getPlayerUuid(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        // TODO Case insentivive? Then also edit the boolean on the next line: false to true/var
        // usernames.Laurenshup.uuid = UUID
        Optional<UUID> optional = getPlayerByUsername(username, false).map(ControllerPlayer::getUuid);
        if (optional.isPresent()) {
            return CompletableFuture.completedFuture(optional);
        }
        // TODO Probably we want to caache!
        return DefaultDataObjects.getUsernamesUuid(dataManager, username);
    }

    /**
     * Gets the network profile from the given UUID.
     * This is done either by taking the network profile from the online player, or by taking it from the data manager.
     *
     * @param uuid the player's UUID
     * @return a {@link CompletableFuture} with an {@link Optional}, if data has been found, the optional is populated with the network profile; otherwise it is empty
     */
    public CompletableFuture<Optional<ControllerProfileData>> getPlayerNetworkProfile(UUID uuid) {
        return getPlayer(uuid).map(ControllerPlayer::getNetworkProfileUuid).map(controller::getProfileData)
                .orElseGet(() -> DefaultDataObjects.getPlayersNetworkProfile(dataManager, uuid).thenCompose(profileIdOpt ->
                        profileIdOpt.map(controller::getProfileData).orElse(CompletableFuture.completedFuture(Optional.empty()))));
    }

    /**
     * Gets the last known username of the player with the given UUID.
     *
     * @param uuid the uuid
     * @return a {@link CompletableFuture} that retrieves an optional that has the username or is empty when no one with the given UUID was online at least once.
     */
    public CompletableFuture<Optional<String>> getOfflinePlayerName(UUID uuid) {
        // TODO Cache?
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return DefaultDataObjects.getPlayersUsername(dataManager, uuid);
    }

    /**
     * Retrieves the saved locale associated with a network profile.
     * If no locale is found, the optional in the returned CompletableFuture will be empty.
     *
     * @param networkProfile the network profile for whom the saved locale is to be fetched; must not be null.
     * @return a {@link CompletableFuture} containing an optional with the profile's saved locale if available, otherwise an empty optional.
     */
    public CompletableFuture<Optional<Locale>> getSavedLocale(ControllerProfileData networkProfile) {
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        if(networkProfile.getType() != ProfileType.NETWORK) throw new IllegalArgumentException("networkProfile must be a network profile");
        return DefaultDataObjects.getNetworkProfilesLocale(dataManager, networkProfile.getId());
    }

    /**
     * Updates the saved locale for a given network profile in the data system with the provided locale.
     *
     * @param networkProfile the network profile. Must not be null.
     * @param locale the new locale to be saved for the network profile. Must not be null.
     * @return a {@link CompletableFuture} that completes once the locale is successfully saved, or completes exceptionally if an error occurs.
     */
    public CompletableFuture<Void> setSavedLocale(ControllerProfileData networkProfile, Locale locale) {
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        Objects.requireNonNull(locale, "locale cannot be null");
        if(networkProfile.getType() != ProfileType.NETWORK) throw new IllegalArgumentException("networkProfile must be a network profile");
        return DefaultDataObjects.setNetworkProfilesLocale(dataManager, networkProfile.getId(), locale);
    }

    protected CompletableFuture<Void> setNickname(UUID networkProfile, String nickname) {
        Objects.requireNonNull(networkProfile, "networkProfile cannot be null");
        return DefaultDataObjects.setNetworkProfilesNickname(dataManager, networkProfile, nickname);
    }

}
