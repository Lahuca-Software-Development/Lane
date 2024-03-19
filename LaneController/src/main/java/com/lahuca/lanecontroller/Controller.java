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

import com.lahuca.lane.connection.Connection;
import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.GameStatusUpdatePacket;
import com.lahuca.lane.connection.packet.PartyPacket;
import com.lahuca.lane.connection.packet.RelationshipPacket;

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
    private final PlayerMethods playerMethods;

    private final Set<ControllerPlayer> players;
    private final Set<ControllerParty> parties;
    private final HashMap<Long, ControllerGame> games = new HashMap<>();

    public Controller(Connection connection, PlayerMethods playerMethods) throws IOException {
        instance = this;

        players = new HashSet<>();
        parties = new HashSet<>();

        this.playerMethods = playerMethods;

        this.connection = connection;
        connection.initialise(input -> {
            Packet packet = input.packet();
            if(packet instanceof GameStatusUpdatePacket gameStatusUpdate) {
                if(!games.containsKey(gameStatusUpdate.gameId())) {
                    // A new game has been created, yeey!
                    games.put(gameStatusUpdate.gameId(),
                            new ControllerGame(gameStatusUpdate.gameId(), input.from(),
                                    gameStatusUpdate.name(), gameStatusUpdate.state()));
                }
                // TODO
            } else if(packet instanceof PartyPacket.Request requestPacket) {
                getParty(requestPacket.partyId()).ifPresent(party -> connection.sendPacket(new PartyPacket.Response(requestPacket.requestId(), party.convertToRecord()), input.from()));
            } else if(packet instanceof RelationshipPacket.Request requestPacket) {
                getPlayer(requestPacket.relationshipId()).flatMap(ControllerPlayer::getRelationship).ifPresent(relationship ->
                        connection.sendPacket(new RelationshipPacket.Response(requestPacket.requestId(), relationship.convertToRecord()), input.from()));
            }
        });
    }

    public Connection getConnection() {
        return connection;
    }

    public void registerGame(ControllerGame controllerGame) {
        games.put(controllerGame.getGameId(), controllerGame);
    }

    public void endGame(ControllerGame controllerGame) {
        endGame(controllerGame.getGameId());
    }

    public void endGame(long id) {
        games.remove(id);
    }

    public void joinPlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        players.add(controllerPlayer);
        controllerPlayer.setGameId(controllerGame.getGameId());
    }

    public void leavePlayer(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        players.remove(controllerPlayer);
    }

    public void creteParty(ControllerPlayer owner, ControllerPlayer invited) {
        ControllerParty controllerParty = new ControllerParty(System.currentTimeMillis(), owner.getUuid());
        controllerParty.sendRequest(invited);
    }

    public void partyWarp(ControllerParty controllerParty, ControllerGame controllerGame) {
        controllerParty.players().forEach(player -> playerMethods.joinServer(player, controllerGame));
    }

    public void partyWarp(ControllerParty controllerParty, long gameId) {
        getGame(gameId).ifPresent(game -> partyWarp(controllerParty, game));
    }

    public void spectateGame(ControllerPlayer controllerPlayer, ControllerGame controllerGame) {
        controllerPlayer.setPlayerState(new ControllerPlayerState("Spectating", new HashMap<>()));
    }

    public Optional<ControllerPlayer> getPlayerByName(String name) {
        return players.stream().filter(player -> player.getName().equals(name)).findFirst();
    }

    public Optional<ControllerPlayer> getPlayer(UUID uuid) {
        return players.stream().filter(player -> player.getUuid().equals(uuid)).findFirst();
    }

    public Optional<ControllerParty> getParty(long id) {
        return parties.stream().filter(party -> party.getPartyId() == id).findFirst();
    }

    public Optional<ControllerGame> getGame(long id) {
        return Optional.ofNullable(games.get(id));
    }

    public Set<ControllerPlayer> getPlayers() {
        return players;
    }

    public Collection<ControllerGame> getGames() {
        return games.values();
    }
}