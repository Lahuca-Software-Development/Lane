/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 20:29 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.queue;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

/**
 * The queue parameter data. The controller decides what to do with the parameter data provided.
 * <p>
 * When this parameter is provided, this is the priority of how data is being looked at when multiple values are given:
 * <ol>
 * <li>Game ID</li>
 * <li>Instance ID & Game Type/Game Map/Game Mode</li>
 * <li>Game Type/Game Map/Game Mode</li>
 * <li>Instance ID</li>
 * <li>Instance Type</li></ol>
 */
public record QueueRequestParameter(HashMap<String, String> data) {

    public Optional<String> instanceId() {
        return Optional.ofNullable(data.get(instanceId));
    }

    public Optional<String> instanceType() {
        return Optional.ofNullable(data.get(instanceType));
    }

    public Optional<Long> gameId() {
        return Optional.ofNullable(data.get(gameId)).map(val -> {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    public Optional<String> gameType() {
        return Optional.ofNullable(data.get(gameType));
    }

    public Optional<String> gameMap() {
        return Optional.ofNullable(data.get(gameMap));
    }

    public Optional<String> gameMode() {
        return Optional.ofNullable(data.get(gameMode));
    }

    public Optional<Boolean> partySkip() {
        return Optional.ofNullable(data.get(partySkip)).map(val -> {
            try {
                return Boolean.parseBoolean(val);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Instead of taking the party skip value from the parameter, it will use some default value if it is not specifically set.
     * The party skip's default value is as follows: {@code true} when joining an instance, {@code false} when joining a game.
     *
     * @return the party skip value or the default value
     */
    public boolean shouldPartySkip() {
        return partySkip().orElse(data.get(gameId) == null && data.get(gameType) == null && data.get(gameMap) == null && data.get(gameMode) == null);
    }

    public Optional<QueueType> queueType() {
        return Optional.ofNullable(data.get(queueType))
                .map(val -> {
                    try {
                        return QueueType.valueOf(val);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
    }

    public Optional<String> extra(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public static final String lobbyInstanceType = "Lobby";

    public static final String instanceId = "INSTANCE.ID"; // Instance ID
    public static final String instanceType = "INSTANCE.TYPE"; // Instance Type
    public static final String gameId = "GAME.ID"; // Game ID
    public static final String gameType = "GAME.TYPE"; // Game Type, example: SkyWars
    public static final String gameMap = "GAME.MAP"; // Game Map, example: Village
    public static final String gameMode = "GAME.MODE"; // Game Mode, example: Singles
    public static final String partySkip = "PARTY.SKIP"; // Whether the party should not be warped (only if owner)
    public static final String queueType = "QUEUE.TYPE"; // Queue Type: default use is PLAYING.


    public static Builder create() {
        return new Builder();
    }

    public static QueueRequestParameter create(Function<Builder, Builder> apply) {
        return apply.apply(new Builder()).build();
    }

    public static QueueRequestParameter lobbyParameter = create().instanceType(lobbyInstanceType).build();

    public static class Builder {

        public static Builder create() {
            return new Builder();
        }

        private final HashMap<String, String> data = new HashMap<>();

        public Builder() {
        }

        public Builder(QueueRequestParameter parameter) {
            data.putAll(parameter.data);
        }

        /**
         * Tells the controller to join a specific instance, if possible.
         * Examples: Lobby1, BedWars-3, etc.
         *
         * @param instanceId The id of the instance.
         * @return This builder.
         */
        public final Builder instanceId(String instanceId) {
            data.put(QueueRequestParameter.instanceId, instanceId);
            return this;
        }

        /**
         * Tells the controller to join a specific game, if possible.
         * Examples: 134558901148, 1249482019393, etc.
         *
         * @param gameId The id of the game.
         * @return This builder.
         */
        public final Builder gameId(long gameId) {
            data.put(QueueRequestParameter.gameId, Long.toString(gameId));
            return this;
        }

        /**
         * Tells the controller to join an instance of the given type.
         * Examples: MainLobby, BedWarsLobby, etc.
         *
         * @param instanceType The instance type.
         * @return This builder.
         */
        public final Builder instanceType(String instanceType) {
            data.put(QueueRequestParameter.instanceType, instanceType);
            return this;
        }

        /**
         * Tells the controller to join a game of the given game type.
         * Examples: BedWars, Parkour, etc.
         *
         * @param gameType The game type.
         * @return This builder.
         */
        public final Builder gameType(String gameType) {
            data.put(QueueRequestParameter.gameType, gameType);
            return this;
        }

        /**
         * Tells the controller to join a game with the given map name.
         * Examples: Flowers, City, etc.
         *
         * @param gameMap The name of the map.
         * @return This builder.
         */
        public final Builder gameMap(String gameMap) {
            data.put(QueueRequestParameter.gameMap, gameMap);
            return this;
        }

        /**
         * Tells the controller to join a game with the given mode.
         * Examples: FFA, Duos, etc.
         *
         * @param gameMode The mode of the game type.
         * @return This builder.
         */
        public final Builder gameMode(String gameMode) {
            data.put(QueueRequestParameter.gameMode, gameMode);
            return this;
        }

        /**
         * Tells the controller to join without/with a player's party.
         * Examples: FFA, Duos, etc.
         *
         * @param partySkip True if the player's party's players should not join as well, false otherwise.
         * @return This builder.
         */
        public final Builder partySkip(boolean partySkip) {
            data.put(QueueRequestParameter.partySkip, Boolean.toString(partySkip)); // TODO Maybe add config setting to change default way of doing?
            return this;
        }

        /**
         * Tells the controller to use a different queue type.
         *
         * @param queueType the queue type
         * @return This builder
         */
        public final Builder queueType(QueueType queueType) {
            data.put(QueueRequestParameter.queueType, queueType.toString());
            return this;
        }

        /**
         * Provide the controller with additional requirements.
         *
         * @param key  The key to store the data at.
         * @param data The data.
         * @return This builder.
         */
        public final Builder extra(String key, String data) {
            this.data.put(key, data);
            return this;
        }

        /**
         * Builds the request parameter.
         *
         * @return The built object.
         */
        public final QueueRequestParameter build() {
            return new QueueRequestParameter(data);
        }

        /**
         * Build a parameters object where the only parameter is this single object.
         *
         * @return the queue request parameters
         */
        public final QueueRequestParameters buildParameters() {
            return QueueRequestParameters.create().add(this).build();
        }

    }

}
