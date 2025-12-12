package com.lahuca.lanecontroller;

import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueStage;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.lanecontroller.events.QueueStageEvent;

import java.util.*;

public class ControllerDefaultQueue {

    /**
     * Handles a default implementation for the single queue stage event.
     * This computes the first result based on solely the given queue parameters.
     * For the different queue types it decides whether the party should be used, and whether some games/instances should be excluded;
     * this is then forwarded to {@link #handleDefaultQueueStageEventParameters(QueueStageEvent, boolean, boolean)}
     *
     * @param event the event to set the default result of
     */
    public static void handleDefaultQueueStageEvent(QueueStageEvent event) {
        switch (event.getQueueRequest().reason()) {
            case NETWORK_JOIN, SERVER_KICKED, GAME_QUIT, GAME_SHUTDOWN -> {
                if (!handleDefaultQueueStageEventParameters(event, false, true)) {
                    event.setDisconnectResult(event.getQueueRequest().reasonMessage());
                }
            }
            case PARTY_JOIN -> handleDefaultQueueStageEventParameters(event, false, true);
            case PLUGIN_INSTANCE, PLUGIN_CONTROLLER -> handleDefaultQueueStageEventParameters(event, true, true);
        }
    }

    /**
     * Fetch the correct games/instances and set the state.
     *
     * @param event        The event
     * @param useParty     If we should also forward parties after this is done
     * @param allowExclude Exclude games/instances that have already been tried
     * @return True if an instance/game has been found
     */
    private static boolean handleDefaultQueueStageEventParameters(QueueStageEvent event, boolean useParty, boolean allowExclude) {
        // Fetch potential party members
        HashMap<UUID, Integer> partyMembers = new HashMap<>();
        Optional<ControllerParty> partyOpt = event.getPlayer().getParty();
        if (useParty && partyOpt.isPresent()) {
            ControllerParty party = partyOpt.get();
            if (party.getOwner().equals(event.getPlayer().getUuid())) {
                // The owner is trying to join, so we should join other players as well
                for (UUID partyMemberUuid : party.getPlayers()) {
                    if (partyMemberUuid.equals(event.getPlayer().getUuid())) continue;
                    Optional<ControllerPlayer> partyMemberOptional = Controller.getPlayer(partyMemberUuid);
                    partyMemberOptional.ifPresent(player -> partyMembers.put(partyMemberUuid, player.getQueuePriority()));
                }
            }
        }

        // Fetch potential excluded instances/games
        HashSet<String> excludeInstances = new HashSet<>();
        HashSet<Long> excludeGames = new HashSet<>();
        if (allowExclude && !event.isInitialRequest()) {
            for (QueueStage stage : event.getQueueRequest().stages()) {
                if (stage.instanceId() != null && stage.gameId() == null) excludeInstances.add(stage.instanceId());
                if (stage.gameId() != null) excludeGames.add(stage.gameId());
            }
        }

        // Actually try to fetch a instance/game
        ArrayList<Set<QueueRequestParameter>> params = event.getQueueRequest().parameters().parameters();
        if (params.isEmpty()) {
            // We do not have the params to know what to do.
            event.setNoneResult();
            return false;
        }
        // Go through each priority
        for (Set<QueueRequestParameter> priority : params) {
            if (priority.isEmpty()) continue;
            ArrayList<QueueRequestParameter> shuffled = new ArrayList<>(priority);
            Collections.shuffle(shuffled);
            // Go through each parameter, but first go through them with kicking disabled.
            // If nothing found, allow kicking
            for (boolean allowKick : List.of(false, true)) {
                for (QueueRequestParameter parameter : shuffled) {
                    HashMap<UUID, Integer> joinTogether = partyMembers;
                    if (parameter.shouldPartySkip()) {
                        joinTogether = new HashMap<>();
                    }
                    QueueType queueType = parameter.queueType().orElse(QueueType.PLAYING);
                    // Find something with this parameter
                    if (parameter.gameId().isPresent()) {
                        Optional<ControllerGame> value = findByGameId(parameter.gameId().get(), excludeInstances, excludeGames, queueType, joinTogether, allowKick);
                        if (value.isPresent()) {
                            if (joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId(), queueType, parameter);
                            else event.setJoinGameResult(value.get().getGameId(), new HashSet<>(joinTogether.keySet()), queueType, parameter);
                            return true;
                        }
                    }
                    if (parameter.gameType().isPresent() || parameter.gameMap().isPresent() || parameter.gameMode().isPresent()) {
                        Optional<ControllerGame> value = findByGameData(parameter.instanceId().orElse(null), parameter.gameType().orElse(null),
                                parameter.gameMap().orElse(null), parameter.gameMode().orElse(null), excludeInstances, excludeGames, queueType, joinTogether, allowKick);
                        if (value.isPresent()) {
                            if (joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId(), queueType, parameter);
                            else
                                event.setJoinGameResult(value.get().getGameId(), new HashSet<>(joinTogether.keySet()), queueType, parameter);
                            return true;
                        }
                    }
                    if (parameter.instanceId().isPresent()) {
                        Optional<ControllerLaneInstance> value = findByInstanceId(parameter.instanceId().get(), excludeInstances, queueType, joinTogether, allowKick);
                        if (value.isPresent()) {
                            if (joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId(), queueType, parameter);
                            else
                                event.setJoinInstanceResult(value.get().getId(), new HashSet<>(joinTogether.keySet()), queueType, parameter);
                            return true;
                        }
                    }
                    if (parameter.instanceType().isPresent()) {
                        Optional<ControllerLaneInstance> value = findByInstanceType(parameter.instanceType().get(), excludeInstances, queueType, joinTogether, allowKick);
                        if (value.isPresent()) {
                            if (joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId(), queueType, parameter);
                            else
                                event.setJoinInstanceResult(value.get().getId(), new HashSet<>(joinTogether.keySet()), queueType, parameter);
                            return true;
                        }
                    }
                }
            }
        }
        // We did not find anything that is left to join for the whole party and that has not tried before.
        event.setNoneResult();
        return false;
    }

    protected static boolean canJoinInstance(Map<UUID, Integer> slots, ControllerLaneInstance instance, QueueType queueType, HashSet<String> excludeInstances, boolean allowKick) {
        if (instance == null) return false;
        if (excludeInstances != null && excludeInstances.contains(instance.getId())) return false;
        if (!instance.hasQueueSlots(slots.keySet(), queueType)) {
            if (!instance.isQueueJoinable(queueType)) return false;
            return allowKick && instance.findKickableLanePlayers(slots, queueType, null, Controller::getPlayer) != null;
        }
        return true;
    }

    protected static boolean canJoinGame(Map<UUID, Integer> slots, ControllerGame game, QueueType queueType, HashSet<String> excludeInstances, boolean allowKick) {
        if (game == null) return false;
        Optional<ControllerLaneInstance> instanceOpt = Controller.getInstance().getInstance(game.getInstanceId());
        if (instanceOpt.isEmpty()) return false;
        ControllerLaneInstance instance = instanceOpt.get();
        if (excludeInstances != null && excludeInstances.contains(instance.getId())) return false;
        boolean kicked = false;
        if (!instance.hasQueueSlots(slots.keySet(), queueType)) {
            if (!instance.isQueueJoinable(queueType)) return false;
            if (!allowKick || instance.findKickableLanePlayers(slots, queueType, game.getGameId(), Controller::getPlayer) == null)
                return false;
            kicked = true;
        }
        if (!game.hasQueueSlots(slots.keySet(), queueType)) {
            if (!game.isQueueJoinable(queueType)) return false;
            if (!kicked) {
                if (!allowKick || game.findKickableLanePlayers(slots, queueType, null, Controller::getPlayer) == null)
                    return false;
            }
        }
        return true;
    }

    private static Optional<ControllerGame> findByGameId(long gameId, HashSet<String> excludeInstances,
                                                         HashSet<Long> excludeGames, QueueType queueType, Map<UUID, Integer> slots, boolean allowKick) {
        if (!excludeGames.contains(gameId)) {
            Optional<ControllerGame> game = Controller.getInstance().getGame(gameId);
            if (canJoinGame(slots, game.orElse(null), queueType, excludeInstances, allowKick)) return game;
        }
        return Optional.empty();
    }

    private static Optional<ControllerGame> findByGameData(String instanceId, String gameType,
                                                           String gameMap, String gameMode, HashSet<String> excludeInstances,
                                                           HashSet<Long> excludeGames, QueueType queueType, Map<UUID, Integer> slots,
                                                           boolean allowKick) {
        if (instanceId != null && excludeInstances.contains(instanceId)) return Optional.empty();
        if (gameType == null && gameMap == null && gameMode == null) {
            return Optional.empty();
        }
        for (ControllerGame game : Controller.getInstance().getGames()) {
            if (game.isPrivate()) continue;
            if (excludeInstances.contains(game.getInstanceId())) continue;
            if (excludeGames.contains(game.getGameId())) continue;
            if (instanceId != null) {
                if (!game.getInstanceId().equals(instanceId)) continue;
            } else {
                if (Controller.getInstance().getInstance(game.getInstanceId()).map(ControllerLaneInstance::isPrivate).orElse(false))
                    continue;
            }
            if (gameType != null && !game.getGameType().equals(gameType)) continue;
            if (gameMap != null && !game.getGameMap().map(c -> c.equals(gameMap)).orElse(false)) continue;
            if (gameMode != null && !game.getGameMode().map(c -> c.equals(gameMode)).orElse(false)) continue;
            // We must have matched the given data. Check if spots are available
            if (canJoinGame(slots, game, queueType, excludeInstances, allowKick)) return Optional.of(game);
        }
        return Optional.empty();
    }

    private static Optional<ControllerLaneInstance> findByInstanceId(String instanceId, HashSet<String> excludeInstances, QueueType queueType, Map<UUID, Integer> slots, boolean allowKick) {
        if (!excludeInstances.contains(instanceId)) {
            Optional<ControllerLaneInstance> instance = Controller.getInstance().getInstance(instanceId);
            if (canJoinInstance(slots, instance.orElse(null), queueType, excludeInstances, allowKick)) return instance;
        }
        return Optional.empty();
    }

    private static Optional<ControllerLaneInstance> findByInstanceType(String instanceType, HashSet<String> excludeInstances, QueueType queueType, Map<UUID, Integer> slots, boolean allowKick) {
        for (ControllerLaneInstance instance : Controller.getInstance().getInstances()) {
            if (instance.isPrivate()) continue;
            if (excludeInstances.contains(instance.getId())) continue;
            if (instance.getType().isEmpty()) continue;
            if (!instance.getType().get().equals(instanceType)) continue;
            if (canJoinInstance(slots, instance, queueType, excludeInstances, allowKick)) return Optional.of(instance);
        }
        return Optional.empty();
    }

}
