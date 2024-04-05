/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 13-3-2024 at 21:31 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.*;
import com.lahuca.lane.connection.request.RequestHandler;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.connection.request.Result;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public class Controller extends RequestHandler {

    private static Controller instance;

    public static Controller getInstance() {
        return instance;
    }

    private final Connection connection;
    private final ControllerImplementation implementation;

    private final HashMap<UUID, ControllerPlayer> players = new HashMap<>();
    private final HashMap<Long, ControllerParty> parties = new HashMap<>();
    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<String, ControllerLaneInstance> instances = new HashMap<>(); // Additional data for the instances
    private final HashMap<Long, ControllerRelationship> relationships = new HashMap<>();


    public Controller(Connection connection, ControllerImplementation implementation) throws IOException {
        instance = this;
        this.connection = connection;
        this.implementation = implementation;

        connection.initialise(input -> {
            Packet iPacket = input.packet();
            if(iPacket instanceof GameStatusUpdatePacket packet) {
                if(!games.containsKey(packet.gameId())) {
                    // A new game has been created, yeey!
                    ControllerGameState initialState = new ControllerGameState(packet.state());
					games.put(packet.gameId(),
							new ControllerGame(packet.gameId(), input.from(),
                                    packet.name(), initialState));
                    connection.sendPacket(new SimpleResultPacket(packet.requestId(), ResponsePacket.OK), input.from());
					return;
				}
                ControllerGame game = games.get(packet.gameId());
                if(!game.getServerId().equals(input.from())) {
                    connection.sendPacket(new SimpleResultPacket(packet.requestId(), ResponsePacket.INSUFFICIENT_RIGHTS), input.from());
                    return;
                }
				games.get(packet.gameId()).update(input.from(), packet.name(), packet.state());
                connection.sendPacket(new SimpleResultPacket(packet.requestId(), ResponsePacket.OK), input.from());
            } else if(iPacket instanceof InstanceStatusUpdatePacket packet) {
                createGetInstance(input.from()).update(packet.joinable(), packet.nonPlayable(), packet.currentPlayers(), packet.maxPlayers());
            } else if(input.packet() instanceof ResponsePacket<?> response) {
                CompletableFuture<Result<?>> request = getRequests().get(response.getRequestId());
                if(request != null) {
                    // TODO How could it happen that the request is null?
                    request.complete(response.transformResult());
                    getRequests().remove(response.getRequestId());
                }
            } else if(iPacket instanceof PartyPacket.Request packet) {
                getParty(packet.partyId()).ifPresent(party -> connection.sendPacket(new PartyPacket.Response(packet.requestId(), party.convertToRecord()), input.from()));
            } else if(iPacket instanceof RelationshipPacket.Request packet) {
                getRelationship(packet.relationshipId()).ifPresent(relationship -> connection.sendPacket(new RelationshipPacket.Response(packet.requestId(), relationship.convertToRecord()), input.from()));
            }
        });
    }

    public Connection getConnection() {
        return connection;
    }

    public ControllerImplementation getImplementation() {
        return implementation;
    }

    private ControllerLaneInstance createGetInstance(String id) {
        if(!instances.containsKey(id)) return instances.put(id, new ControllerLaneInstance(id));
        return instances.get(id);
    }

    private Optional<ControllerLaneInstance> getInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    }

    public void registerPlayer(ControllerPlayer player) {
        if(player == null || player.getUuid() == null) return;
        if(players.containsKey(player.getUuid())) return;
        players.put(player.getUuid(), player);
    }

    public void unregisterPlayer(UUID player) {
        players.remove(player);
    }

    /**
     * Sends the given players to the instance.
     * If the players are trying to join a game, it will also send the game they are willing/trying to join.
     * @param players the players
     * @param destination the instance id
     * @param gameId the game id
     */
    private CompletableFuture<Result<Void>> sendToInstance(Set<ControllerPlayer> players, String destination, Long gameId) {
        // Check whether the input parameters are correct
        if(destination == null || players.isEmpty()) return simpleFuture(ResponsePacket.INVALID_PARAMETERS);
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        // Check if we have a party and instance with the given IDs
        getInstance(destination).ifPresentOrElse(instance -> {
            // Check whether the instance allows the party to join
            if(!instance.isJoinable()) {
                future.complete(simpleResult(ResponsePacket.NOT_JOINABLE));
                return;
            }
            // TODO party.getPlayers() might contain offline players
            if(instance.getCurrentPlayers() + players.size() > instance.getMaxPlayers()) { // TODO Add override
                // TODO Maybe partially tp party
                future.complete(simpleResult(ResponsePacket.NO_FREE_SLOTS));
                return;
            }
            // First try to reservate spots for the players
            String stateName = gameId == null ? LanePlayerState.INSTANCE_TRANSFER : LanePlayerState.GAME_TRANSFER;
            HashSet<ControllerStateProperty> parameters = new HashSet<>();
            parameters.add(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, destination));
            if(gameId != null) parameters.add(new ControllerStateProperty(LaneStateProperty.GAME_ID, destination));
            ControllerPlayerState state = new ControllerPlayerState(stateName, parameters);
            CompletableFuture<Result<Void>> last = null;
            for(ControllerPlayer player : players) {
                player.setState(state);
                long id = getNewRequestId();
                CompletableFuture<Result<Void>> result = buildVoidFuture(id);
                // TODO Override slots boolean! in line below
                connection.sendPacket(new InstanceJoinPacket(id, player.convertRecord(), false, null), destination);
                if(last == null) {
                    last = result;
                } else {
                    last = last.thenCombine(result, (first, second) -> {
                        if(first.isSuccesful()) return second;
                        return first;
                    });
                }
            }
            if(last == null) {
                // Somehow we do not have players in our party, so probably invalid ID.
                // TODO Undo the join at the instance
                future.complete(simpleResult(ResponsePacket.INVALID_ID));
                return;
            }
            last.whenComplete((result, ex) -> {
                if(ex != null) {
                    future.completeExceptionally(ex);
                    return;
                }
                if(!result.isSuccesful()) {
                    // Do not send players, cancel join by sending packets
                    // TODO Undo the join at the instance
                    future.complete(result);
                } else {
                    // The instance allows all joins to happen, so do it
                    CompletableFuture<Result<Void>> joinLast = null;
                    for(ControllerPlayer player : players) {
                        CompletableFuture<Result<Void>> joinResult = implementation.joinServer(player.getUuid(), destination);
                        if(joinLast == null) {
                            joinLast = joinResult;
                        } else {
                            joinLast = joinLast.thenCombine(joinResult, (first, second) -> {
                                if(first.result().equals(ResponsePacket.OK)) {
                                    if(second.result().equals(ResponsePacket.OK)) {
                                        return simpleResult(ResponsePacket.OK);
                                    }
                                    return simpleResult(ResponsePacket.OK_PARTIALLY);
                                }
                                if(second.result().equals(ResponsePacket.OK)) {
                                    return simpleResult(ResponsePacket.OK_PARTIALLY);
                                }
                                return first;
                            });
                        }
                    }
                    if(joinLast == null) {
                        // Somehow we do not have players in our party, so probably invalid ID.
                        future.complete(simpleResult(ResponsePacket.INVALID_ID));
                        return;
                    }
                    joinLast.whenComplete((joinResult, joinEx) -> {
                        if(joinEx == null) future.complete(joinResult);
                        else future.completeExceptionally(joinEx);
                    });
                }
            });
        }, () -> future.complete(simpleResult(ResponsePacket.INVALID_ID)));
        return future;
    }

    /**
     * A player is willing/trying to join an instance.
     * This will send the data to the respective instance, and teleport the player to there.
     * @param player the player
     * @param destination the instance id
     * @return the result of the join
     */
    public CompletableFuture<Result<Void>> joinInstance(UUID player, String destination) {
        // Check whether the input parameters are correct
        if(player == null || destination == null) return simpleFuture(ResponsePacket.INVALID_PARAMETERS);
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        getPlayer(player).ifPresentOrElse(current -> {
            sendToInstance(Set.of(current), destination, null).whenComplete((result, ex) -> {
                if(ex == null) future.complete(result);
                else future.completeExceptionally(ex);
            });
        }, () -> future.complete(simpleResult(ResponsePacket.INVALID_ID)));
        return future;
    }

    /**
     * A party is willing/trying to join an instance.
     * This will send the data to the respective instance, and teleport the players to there.
     * @param partyId the party's id
     * @param destination the instance id
     * @return the result of the join
     */
    public CompletableFuture<Result<Void>> joinInstance(long partyId, String destination) {
        // Check whether the input parameters are correct
        if(destination == null) return simpleFuture(ResponsePacket.INVALID_PARAMETERS);
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        // Check if we have a party and instance with the given IDs
        getParty(partyId).ifPresentOrElse(party -> {
            HashSet<ControllerPlayer> players = new HashSet<>();
            for(UUID uuid : party.getPlayers()) {
                getPlayer(uuid).ifPresent(players::add);
            }
            sendToInstance(players, destination, null).whenComplete((result, ex) -> {
                if(ex == null) future.complete(result);
                else future.completeExceptionally(ex);
            });
        }, () -> future.complete(simpleResult(ResponsePacket.INVALID_ID)));
        return future;
    }

    /**
     * A player is willing/trying to join a game on an instance.
     * This will first check whether the game is joinable at the current moment.
     * When it is joinable, it will check whether the player has a party, and is the party owner.
     * In that case it will join the whole party, otherwise only the player.
     * Joining meaning: send the respective data and transfer player(s)
     * @param player the player
     * @param gameId the game id
     * @return the result of the join
     */
    public CompletableFuture<Result<Void>> joinGame(UUID player, long gameId) {
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        getGame(gameId).ifPresentOrElse(game -> {
            if(game.getServerId() == null || !game.getState().isJoinable()) {
                future.complete(simpleResult(ResponsePacket.INVALID_ID));
                return;
            }
            getPlayer(player).ifPresent(owner -> {
                Runnable withoutParty = () -> sendToInstance(Set.of(owner), game.getServerId(), gameId).whenComplete((result, ex) -> {
                    if(ex == null) future.complete(result);
                    else future.completeExceptionally(ex);
                });
                owner.getPartyId().ifPresentOrElse(partyId -> getParty(partyId).ifPresentOrElse(party -> {
                    if(party.getOwner().equals(player)) joinGame(partyId, gameId);
                    else withoutParty.run();
                }, withoutParty), withoutParty);
            });
        }, () -> future.complete(simpleResult(ResponsePacket.INVALID_ID)));
        return future;
    }

    /**
     * A party is willing/trying to join a game on an instance.
     * This will first check whether the game is joinable at the current moment.
     * When it is joinable, it will send the respective data and transfer the party to there.
     * @param partyId the party's id
     * @param gameId the game id
     * @return the result of the join
     */
    public CompletableFuture<Result<Void>> joinGame(long partyId, long gameId) {
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        getGame(gameId).ifPresent(game -> {
            if(game.getServerId() == null || !game.getState().isJoinable()) {
                future.complete(simpleResult(ResponsePacket.INVALID_ID));
                return;
            }
            getParty(partyId).ifPresent(party -> {
                HashSet<ControllerPlayer> players = new HashSet<>();
                party.getPlayers().forEach(uuid -> getPlayer(uuid).ifPresent(players::add));
                sendToInstance(players, game.getServerId(), gameId).whenComplete((result, ex) -> {
                    if(ex == null) future.complete(result);
                    else future.completeExceptionally(ex);
                });
            });
        });
        return future;
    }

    public void endGame(long id) { // TODO Check
        games.remove(id);
    }


    public void leavePlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        players.remove(controllerPlayer.getUuid());
    }

    public void createParty(ControllerPlayer owner, ControllerPlayer invited) {
        ControllerParty controllerParty = new ControllerParty(System.currentTimeMillis(), owner.getUuid());
        controllerParty.sendRequest(invited);
    }

    public void createRelationship(ControllerPlayer... players) {
        long id = System.currentTimeMillis();
        Set<UUID> uuids = new HashSet<>();
        Arrays.stream(players).forEach(controllerPlayer -> uuids.add(controllerPlayer.getUuid()));
        relationships.put(id, new ControllerRelationship(id, uuids));
    }

    public Collection<ControllerPlayer> getPlayers() {
        return players.values();
    }

    public Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public Optional<ControllerPlayer> getPlayerByName(String name) {
        return players.values().stream().filter(player -> player.getName().equals(name)).findFirst();
    }

    public Collection<ControllerParty> getParties() {
        return parties.values();
    }

    public Optional<ControllerRelationship> getRelationship(long id) {
        return Optional.ofNullable(relationships.get(id));
    }

    public Optional<ControllerParty> getParty(long id) {
        return Optional.ofNullable(parties.get(id));
    }

    public Collection<ControllerGame> getGames() {
        return games.values();
    }

    public Optional<ControllerGame> getGame(long id) {
        return Optional.ofNullable(games.get(id));
    }
}