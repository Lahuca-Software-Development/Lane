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
     * @param event the event to set the default result of
     */
    public static void handleDefaultQueueStageEvent(QueueStageEvent event) {
        switch(event.getQueueRequest().reason()) {
            case NETWORK_JOIN, SERVER_KICKED -> {
                if(!handleDefaultQueueStageEventParameters(event, false, true)) {
                    event.setDisconnectResult(); // TODO Maybe add message?
                }
            }
            case PARTY_JOIN -> handleDefaultQueueStageEventParameters(event, false, true);
            case PLUGIN_INSTANCE, PLUGIN_CONTROLLER -> handleDefaultQueueStageEventParameters(event, true, true);
        }
    }

    /**
     * Fetch the correct games/instances and set the state.
     * @param event The event
     * @param useParty If we should also forward parties after this is done
     * @param allowExclude Exclude games/instances that have already been tried
     * @return True if an instance/game has been found
     */
    private static boolean handleDefaultQueueStageEventParameters(QueueStageEvent event, boolean useParty, boolean allowExclude) {
        // Fetch potential party members
        HashSet<UUID> partyMembers = new HashSet<>();
        Optional<ControllerParty> partyOpt = event.getPlayer().getParty();
        if(useParty && partyOpt.isPresent()) {
            ControllerParty party = partyOpt.get();
            if (party.getOwner().equals(event.getPlayer().getUuid())) {
                // The owner is trying to join, so we should join other players as well
                for (UUID partyMemberUuid : party.getPlayers()) {
                    Optional<ControllerPlayer> partyMemberOptional = Controller.getPlayer(partyMemberUuid);
                    partyMemberOptional.ifPresent(player -> partyMembers.add(partyMemberUuid));
                }
            }
        }

        // Fetch potential excluded instances/games
        HashSet<String> excludeInstances = new HashSet<>();
        HashSet<Long> excludeGames = new HashSet<>();
        if(allowExclude && !event.isInitialRequest()) {
            for(QueueStage stage : event.getQueueRequest().stages()) {
                if(stage.instanceId() != null && stage.gameId() == null) excludeInstances.add(stage.instanceId());
                if(stage.gameId() != null) excludeGames.add(stage.gameId());
            }
        }

        // Actually try to fetch a instance/game
        ArrayList<Set<QueueRequestParameter>> params = event.getQueueRequest().parameters().parameters();
        if(params.isEmpty()) {
            // We do not have the params to know what to do.
            event.setNoneResult();
            return false;
        }
        for(Set<QueueRequestParameter> priority : params) {
            if(priority.isEmpty()) continue;
            ArrayList<QueueRequestParameter> shuffled = new ArrayList<>(priority);
            Collections.shuffle(shuffled);
            for(QueueRequestParameter parameter : shuffled) {
                HashSet<UUID> joinTogether = partyMembers;
                if(parameter.partySkip().orElse(false)) {
                    joinTogether = new HashSet<>();
                }
                QueueType queueType = parameter.queueType().orElse(QueueType.PLAYING);
                // Find something with this parameter
                if(parameter.gameId().isPresent()) {
                    Optional<ControllerGame> value = findByGameId(parameter.gameId().get(), excludeInstances, excludeGames, queueType, joinTogether.size());
                    if(value.isPresent()) {
                        if(joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId());
                        else event.setJoinGameResult(value.get().getGameId(), joinTogether, queueType);
                        return true;
                    }
                }
                if(parameter.gameType().isPresent() || parameter.gameMap().isPresent() || parameter.gameMode().isPresent()) {
                    Optional<ControllerGame> value = findByGameData(parameter.instanceId().orElse(null), parameter.gameType().orElse(null),
                            parameter.gameMap().orElse(null), parameter.gameMode().orElse(null), excludeInstances, excludeGames, queueType, joinTogether.size());
                    if(value.isPresent()) {
                        if(joinTogether.isEmpty()) event.setJoinGameResult(value.get().getGameId());
                        else event.setJoinGameResult(value.get().getGameId(), joinTogether, queueType);
                        return true;
                    }
                }
                if(parameter.instanceId().isPresent()) {
                    Optional<ControllerLaneInstance> value = findByInstanceId(parameter.instanceId().get(), excludeInstances, queueType, joinTogether.size());
                    if(value.isPresent()) {
                        if(joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId());
                        else event.setJoinInstanceResult(value.get().getId(), joinTogether, queueType);
                        return true;
                    }
                }
                if(parameter.instanceType().isPresent()) {
                    Optional<ControllerLaneInstance> value = findByInstanceType(parameter.instanceType(), excludeInstances, queueType, joinTogether.size());
                    if(value.isPresent()) {
                        if(joinTogether.isEmpty()) event.setJoinInstanceResult(value.get().getId());
                        else event.setJoinInstanceResult(value.get().getId(), joinTogether, queueType);
                        return true;
                    }
                }
            }
        }
        // We did not find anything that is left to join for the whole party and that has not tried before.
        event.setNoneResult();
        return false;
    }

    private static Optional<ControllerGame> findByGameId(long gameId, HashSet<String> excludeInstances,
                                                  HashSet<Long> excludeGames, QueueType queueType, int spots) {
        if(!excludeGames.contains(gameId)) {
            Optional<ControllerGame> game = Controller.getInstance().getGame(gameId);
            if(game.isPresent()) {
                Optional<ControllerLaneInstance> instance = Controller.getInstance().getInstance(game.get().getInstanceId());
                return Optional.ofNullable(instance.isPresent() && !excludeInstances.contains(instance.get().getId()) && instance.get().isQueueJoinable(queueType, spots) ? game.get() : null)
                        .filter(current -> current.isQueueJoinable(queueType, spots));
            }
        }
        return Optional.empty();
    }

    private static Optional<ControllerGame> findByGameData(String instanceId, String gameType,
                                                    String gameMap, String gameMode, HashSet<String> excludeInstances,
                                                    HashSet<Long> excludeGames, QueueType queueType, int spots) {
        if(instanceId != null && excludeInstances.contains(instanceId)) return Optional.empty();
        if(gameType == null && gameMap == null && gameMode == null) {
            return Optional.empty();
        }
        for(ControllerGame game : Controller.getInstance().getGames()) {
            if(excludeInstances.contains(game.getInstanceId())) continue;
            if(excludeGames.contains(game.getGameId())) continue;
            if(instanceId != null && !game.getInstanceId().equals(instanceId)) continue;
            if(gameType != null && !game.getGameType().equals(gameType)) continue;
            if(gameMap != null && !game.getGameMap().map(c -> c.equals(gameMap)).orElse(false)) continue;
            if(gameMode != null && !game.getGameMode().map(c -> c.equals(gameMode)).orElse(false)) continue;
            // We must have matched the given data. Check if spots are available
            Optional<ControllerLaneInstance> instance = Controller.getInstance().getInstance(instanceId);
            if(game.isQueueJoinable(queueType, spots) && instance.isPresent() && instance.get().isQueueJoinable(queueType, spots)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    private static Optional<ControllerLaneInstance> findByInstanceId(Object instanceIdObject, HashSet<String> excludeInstances, QueueType queueType, int spots) {
        if(instanceIdObject instanceof String instanceId && !excludeInstances.contains(instanceId)) {
            Optional<ControllerLaneInstance> instance = Controller.getInstance().getInstance(instanceId);
            if(instance.isPresent()) {
                return Optional.ofNullable(instance.get().isQueueJoinable(queueType, spots) ? instance.get() : null);
            }
        }
        return Optional.empty();
    }

    private static Optional<ControllerLaneInstance> findByInstanceType(Object instanceTypeObject, HashSet<String> excludeInstances, QueueType queueType, int spots) {
        if(instanceTypeObject instanceof String instanceType) {
            for(ControllerLaneInstance instance : Controller.getInstance().getInstances()) {
                if(excludeInstances.contains(instance.getId())) continue;
                if(instance.getType().isEmpty()) continue;
                if(instance.getType().get().equals(instanceType) && instance.isQueueJoinable(queueType, spots)) {
                    return Optional.of(instance);
                }
            }
        }
        return Optional.empty();
    }

}
