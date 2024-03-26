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
import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;
import com.lahuca.lane.connection.packet.InstanceJoinPacket;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.packet.RelationshipPacket;
import com.lahuca.lane.records.PlayerRecord;

import java.io.IOException;
import java.util.*;

/**
 * This is the main class for operations on the controller side of the Lane system.
 */
public class Controller {

    private static Controller instance;

    public static Controller getInstance() {
        return instance;
    }

    private final Connection connection;
    private final ControllerImplementation implementation;

    private final HashMap<Long, ControllerGame> games = new HashMap<>(); // Games are only registered because of instances
    private final HashMap<UUID, ControllerPlayer> players = new HashMap<>();
    private final HashMap<Long, ControllerParty> parties = new HashMap<>();
    private final HashMap<Long, ControllerRelationship> relationships = new HashMap<>();


    public Controller(Connection connection, ControllerImplementation implementation) throws IOException {
        instance = this;
        this.connection = connection;
        this.implementation = implementation;

        connection.initialise(input -> {
            Packet packet = input.packet();
            if(packet instanceof GameStatusUpdatePacket gameStatusUpdate) {
                if(!games.containsKey(gameStatusUpdate.gameId())) {
                    // A new game has been created, yeey!
                    ControllerGameState initialState = new ControllerGameState(gameStatusUpdate.state());
                    games.put(gameStatusUpdate.gameId(),
                            new ControllerGame(gameStatusUpdate.gameId(), input.from(),
                                    gameStatusUpdate.name(), initialState));
                    return;
                }
                games.get(gameStatusUpdate.gameId()).update(input.from(), gameStatusUpdate.name(), gameStatusUpdate.state());
            } else if(packet instanceof PartyPacket.Request requestPacket) {
                getParty(requestPacket.partyId()).ifPresent(party -> connection.sendPacket(new PartyPacket.Response(requestPacket.requestId(), party.convertToRecord()), input.from()));
            } else if(packet instanceof RelationshipPacket.Request requestPacket) {
                getRelationship(requestPacket.relationshipId()).ifPresent(relationship ->
                        connection.sendPacket(new RelationshipPacket.Response(requestPacket.requestId(), relationship.convertToRecord()), input.from()));
            }
        });
    }

    public Connection getConnection() {
        return connection;
    }

    public ControllerImplementation getImplementation() {
        return implementation;
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
     *
     * @param players     the players
     * @param destination the instance id
     * @param gameId      the game id
     */
    private void sendToInstance(Set<ControllerPlayer> players, String destination, Long gameId) {
        if(destination == null || players.isEmpty()) return;
        PlayerRecord[] records = new PlayerRecord[players.size()];
        int index = 0;
        // Build state change
        String stateName = gameId == null ? LanePlayerState.INSTANCE_JOIN : LanePlayerState.GAME_JOIN;
        HashSet<ControllerStateProperty> properties = new HashSet<>();
        properties.add(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, destination));
        if(gameId != null) properties.add(new ControllerStateProperty(LaneStateProperty.GAME_ID, gameId));
        ControllerPlayerState state = new ControllerPlayerState(stateName, properties);
        // Set state, fetch records
        for(ControllerPlayer player : players) {
            player.setState(state);
            records[index] = player.convertRecord();
            index++;
        }
        // Send data to instance, join player
        connection.sendPacket(new InstanceJoinPacket(records, gameId), destination);
        players.forEach(player -> implementation.joinServer(player.getUuid(), destination));
    }

    /**
     * A player is willing/trying to join an instance.
     * This will send the data to the respective instance, and teleport the player to there.
     *
     * @param player      the player
     * @param destination the instance id
     */
    public void joinInstance(UUID player, String destination) {
        if(player == null || destination == null) return;
        // TODO Do we maybe want a feature that checks whether there is room left on an instance?
        getPlayer(player).ifPresent(current -> {
            HashSet<ControllerPlayer> players = new HashSet<>();
            players.add(current);
            sendToInstance(players, destination, null);
        });
        // TODO Why not return the end state? Either in return, asyned consumer, or future?
    }

    /**
     * A party is willing/trying to join an instance.
     * This will send the data to the respective instance, and teleport the players to there.
     *
     * @param partyId     the party's id
     * @param destination the instance id
     */
    public void joinInstance(long partyId, String destination) {
        if(destination == null) return;
        getParty(partyId).ifPresent(party -> {
            HashSet<ControllerPlayer> players = new HashSet<>();
            party.getPlayers().forEach(uuid -> getPlayer(uuid).ifPresent(players::add)); // TODO Parties could have offline players
            if(players.isEmpty()) return;
            // TODO Do we maybe want a feature that checks whether there is room left on an instance?
            sendToInstance(players, destination, null);
        });
        // TODO Why not return the end state? Either in return, asyned consumer, or future?
    }

    /**
     * A player is willing/trying to join a game on an instance.
     * This will first check whether the game is joinable at the current moment.
     * When it is joinable, it will check whether the player has a party, and is the party owner.
     * In that case it will join the whole party, otherwise only the player.
     * Joining meaning: send the respective data and transfer player(s)
     *
     * @param player the player
     * @param gameId the game id
     */
    public void joinGame(UUID player, long gameId) {
        getGame(gameId).ifPresent(game -> {
            if(game.getServerId() == null || !game.getState().isJoinable()) return;
            // TODO Check whether there are players left? Maybe in state properties
            getPlayer(player).ifPresent(owner -> {
                // TODO Do we maybe want a feature that checks whether there is room left on an instance?
                Runnable withoutParty = () -> sendToInstance(Set.of(owner), game.getServerId(), gameId);
                owner.getPartyId().ifPresentOrElse(partyId -> getParty(partyId).ifPresentOrElse(party -> {
                    if(party.getOwner().equals(player)) joinGame(partyId, gameId);
                    else withoutParty.run();
                }, withoutParty), withoutParty);
            });
        });
        // TODO Why not return the end state? Either in return, asyned consumer, or future?
    }

    /**
     * A party is willing/trying to join a game on an instance.
     * This will first check whether the game is joinable at the current moment.
     * When it is joinable, it will send the respective data and transfer the party to there.
     *
     * @param partyId the party's id
     * @param gameId  the game id
     */
    public void joinGame(long partyId, long gameId) {
        getGame(gameId).ifPresent(game -> {
            if(game.getServerId() == null || !game.getState().isJoinable()) return;
            // TODO Check whether there are players left? Maybe in state properties
            getParty(partyId).ifPresent(party -> {
                HashSet<ControllerPlayer> players = new HashSet<>();
                party.getPlayers().forEach(uuid -> getPlayer(uuid).ifPresent(players::add)); // TODO Parties could have offline players
                if(players.isEmpty()) return;
                // TODO Do we maybe want a feature that checks whether there is room left on an instance?
                sendToInstance(players, game.getServerId(), gameId);
            });
        });
        // TODO Why not return the end state? Either in return, asyned consumer, or future?
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

    public void disbandParty(ControllerParty party) {
        parties.remove(party.getId());
        party.disband();
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