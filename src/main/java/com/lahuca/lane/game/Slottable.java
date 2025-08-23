package com.lahuca.lane.game;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.queue.QueueType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Slottable {

    // TODO Auto clear reserved slots, when it goes invalid after X seconds? Better handling?
    HashSet<UUID> getReserved(); // Reserved slots

    HashSet<UUID> getOnline(); // All online players = reserved minus still connecting.

    HashSet<UUID> getPlayers(); // Only the actual players = online - vanished admins

    HashSet<UUID> getPlaying(); // Only the actual playing players = players - spectators/viewers

    boolean isOnlineJoinable();

    boolean isPlayersJoinable();

    boolean isPlayingJoinable();

    int getMaxOnlineSlots();

    int getMaxPlayersSlots();

    int getMaxPlayingSlots();

    boolean isOnlineKickable();

    boolean isPlayersKickable();

    boolean isPlayingKickable();

    default int getAvailableOnlineSlots() {
        if (getMaxOnlineSlots() <= 0) return Integer.MAX_VALUE;
        return getMaxOnlineSlots() - getReserved().size();
    }

    default int getAvailablePlayersSlots() {
        if (getMaxPlayersSlots() <= 0) return Integer.MAX_VALUE;
        return getMaxPlayersSlots() - getPlayers().size();
    }

    default int getAvailablePlayingSlots() {
        if (getMaxPlayingSlots() <= 0) return Integer.MAX_VALUE;
        return getMaxPlayingSlots() - getPlaying().size();
    }

    default boolean isQueueJoinable(QueueType queueType) {
        return switch (queueType) {
            case ONLINE -> isOnlineJoinable();
            case PLAYERS -> isOnlineJoinable() && isPlayersJoinable();
            case PLAYING -> isOnlineJoinable() && isPlayersJoinable() && isPlayingJoinable();
        };
    }

    /**
     * Computes whether the given players can be granted a slot in the given list.
     * If a player is already in the list, it is already granted it.
     * @param players the players as a set of their UUIDs
     * @param queueType the list type
     * @return {@code true} if the player can be granted a slot, {@code false} otherwise
     */
    default boolean hasQueueSlots(Set<UUID> players, QueueType queueType) {
        int alreadyOnline = 0;
        int alreadyPlayers = 0;
        int alreadyPlaying = 0;
        for (UUID uuid : players) {
            if(containsOnline(uuid)) alreadyOnline++;
            if(containsPlayers(uuid)) alreadyPlayers++;
            if(containsPlaying(uuid)) alreadyPlaying++;
        }
        int slots = players.size();
        return switch (queueType) {
            case ONLINE -> isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= slots - alreadyOnline);
            case PLAYERS -> (isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= slots - alreadyOnline))
                    && (isPlayersJoinable() && (getMaxPlayersSlots() <= 0 || getAvailablePlayersSlots() >= slots - alreadyPlayers));
            case PLAYING -> (isOnlineJoinable() && (getMaxOnlineSlots() <= 0 || getAvailableOnlineSlots() >= slots - alreadyOnline))
                    && (isPlayersJoinable() && (getMaxPlayersSlots() <= 0 || getAvailablePlayersSlots() >= slots - alreadyPlayers))
                    && (isPlayingJoinable() && (getMaxPlayingSlots() <= 0 || getAvailablePlayingSlots() >= slots - alreadyPlaying));
        };
    }

    default boolean containsReserved(UUID uuid) {
        return getReserved().contains(uuid);
    }

    default boolean containsOnline(UUID uuid) {
        return getOnline().contains(uuid);
    }

    default boolean containsPlayers(UUID uuid) {
        return getPlayers().contains(uuid);
    }

    default boolean containsPlaying(UUID uuid) {
        return getPlaying().contains(uuid);
    }

    default LinkedHashSet<LanePlayer> getOnlineSortedQueuePriority(Function<UUID, Optional<? extends LanePlayer>> uuidToPlayer) {
        LinkedHashSet<LanePlayer> map = new LinkedHashSet<>();
        for (UUID uuid : getOnline()) {
            uuidToPlayer.apply(uuid).ifPresent(map::add);
        }
        return sortLanePlayersSet(map);
    }

    default LinkedHashSet<LanePlayer> getPlayersSortedQueuePriority(Function<UUID, Optional<? extends LanePlayer>> uuidToPlayer) {
        LinkedHashSet<LanePlayer> map = new LinkedHashSet<>();
        for (UUID uuid : getPlayers()) {
            uuidToPlayer.apply(uuid).ifPresent(map::add);
        }
        return sortLanePlayersSet(map);
    }

    default LinkedHashSet<LanePlayer> getPlayingSortedQueuePriority(Function<UUID, Optional<? extends LanePlayer>> uuidToPlayer) {
        LinkedHashSet<LanePlayer> map = new LinkedHashSet<>();
        for (UUID uuid : getPlaying()) {
            uuidToPlayer.apply(uuid).ifPresent(map::add);
        }
        return sortLanePlayersSet(map);
    }

    @NotNull
    private LinkedHashSet<LanePlayer> sortLanePlayersSet(LinkedHashSet<LanePlayer> set) {
        return set.stream()
                .sorted(Comparator.comparingInt(LanePlayer::getQueuePriority))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @NotNull
    private LinkedHashMap<UUID, Integer> sortValueSet(LinkedHashMap<UUID, Integer> set) {
        return set.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Selects a set of players from the kickable set that so that all spots can be filled.
     * @param toReplace the players that need a slot, the value is their queue priority, needs to be sorted by lowest queue priority
     * @param toKick the players that can be kicked, needs to be sorted by lowest queue priority
     * @param gameId the game ID where a place should be found
     * @return a set with the players that can be kicked, null if not possible
     */
    private HashSet<LanePlayer> findKickableLanePlayers(LinkedHashMap<UUID, Integer> toReplace, LinkedHashSet<LanePlayer> toKick, Long gameId) {
        HashSet<LanePlayer> kickable = new HashSet<>();
        toReplace.forEach((replace, priority) -> {
            Iterator<LanePlayer> kickIterator = toKick.iterator();
            while (kickIterator.hasNext()) {
                LanePlayer kick = kickIterator.next();
                if(kick.getQueuePriority() < priority && Objects.equals(kick.getGameId().orElse(null), gameId)) {
                    kickable.add(kick);
                    kickIterator.remove();
                    break;
                }
            }
        });
        return kickable.size() == toReplace.size() ? kickable : null;
    }

    /**
     * Finds kickable {@link LanePlayer}s that can be kicked in order for the given parameters to join.
     * @param join the map of players that are wanting to join, with their queue priority as value
     * @param queueType the queue type to add to
     * @param gameId the game ID where a place should be found
     * @param uuidToPlayer a function that can be used to retrieve a {@link LanePlayer} from a {@link UUID}
     * @return the players that can be kicked, or null if not possible
     */
    default HashSet<LanePlayer> findKickableLanePlayers(Map<UUID, Integer> join, QueueType queueType, Long gameId, Function<UUID, Optional<? extends LanePlayer>> uuidToPlayer) {
        // Compute how many slots are needed
        HashSet<UUID> alreadyOnline = new HashSet<>();
        HashSet<UUID> alreadyPlayers = new HashSet<>();
        HashSet<UUID> alreadyPlaying = new HashSet<>();
        for (UUID current : join.keySet()) {
            if(containsOnline(current)) alreadyOnline.add(current);
            if(containsPlayers(current)) alreadyPlayers.add(current);
            if(containsPlaying(current)) alreadyPlaying.add(current);
        }

        if(getAvailablePlayingSlots() < join.size() - alreadyPlaying.size() && queueType == QueueType.PLAYING) {
            // We have to clear up playing
            if(!isPlayingKickable()) return null;
            // We can kick, find (size - alreadyPlaying) kickable players
            LinkedHashMap<UUID, Integer> findSpots = new LinkedHashMap<>(join);
            findSpots.keySet().removeAll(alreadyPlaying);
            findSpots = sortValueSet(findSpots);
            LinkedHashSet<LanePlayer> set = getPlayingSortedQueuePriority(uuidToPlayer);
            alreadyPlaying.forEach(uuid -> set.removeIf(player -> player.getUuid().equals(uuid)));
            return findKickableLanePlayers(findSpots, set, gameId);
        }
        if(getAvailablePlayersSlots() == join.size() - alreadyPlayers.size() && (queueType == QueueType.PLAYERS || queueType == QueueType.PLAYING)) {
            // We have to clear up players
            if(!isPlayersKickable()) return null;
            // We can kick, find (size - alreadyPlayers) kickable players
            LinkedHashMap<UUID, Integer> findSpots = new LinkedHashMap<>(join);
            findSpots.keySet().removeAll(alreadyPlayers);
            findSpots = sortValueSet(findSpots);
            LinkedHashSet<LanePlayer> set = getPlayersSortedQueuePriority(uuidToPlayer);
            alreadyPlayers.forEach(uuid -> set.removeIf(player -> player.getUuid().equals(uuid)));
            return findKickableLanePlayers(findSpots, set, gameId);
        }
        if(getAvailableOnlineSlots() == join.size() - alreadyOnline.size()) {
            // We have to clear up online
            if(!isOnlineKickable()) return null;
            // We can kick, find (size - alreadyPlayers) kickable players
            LinkedHashMap<UUID, Integer> findSpots = new LinkedHashMap<>(join);
            findSpots.keySet().removeAll(alreadyOnline);
            findSpots = sortValueSet(findSpots);
            LinkedHashSet<LanePlayer> set = getOnlineSortedQueuePriority(uuidToPlayer);
            alreadyOnline.forEach(uuid -> set.removeIf(player -> player.getUuid().equals(uuid)));
            return findKickableLanePlayers(findSpots, set, gameId);
        }
        return null;
    }

}
