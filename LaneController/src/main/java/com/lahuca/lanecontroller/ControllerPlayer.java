/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:30 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontroller;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.LaneStateProperty;
import com.lahuca.lane.connection.packet.InstanceJoinPacket;
import com.lahuca.lane.connection.packet.InstanceUpdatePlayerPacket;
import com.lahuca.lane.connection.request.ResultUnsuccessfulException;
import com.lahuca.lane.queue.*;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lanecontroller.events.QueueStageEvent;
import com.lahuca.lanecontroller.events.QueueStageEventResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ControllerPlayer implements LanePlayer { // TODO Maybe make generic: ControllerPlayer<T> where T stands for the implemented object in Velocity: Player

    private final UUID uuid;
    private final String username;
    private String displayName;
    private QueueRequest queueRequest;
    private String instanceId = null;
    private Long gameId = null;
    private ControllerPlayerState state = null;
    private Long partyId = null;

    ControllerPlayer(UUID uuid, String username, String displayName) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = displayName;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Optional<QueueRequest> getQueueRequest() {
        return Optional.ofNullable(queueRequest);
    }

    @Override
    public Optional<String> getInstanceId() {
        return Optional.ofNullable(instanceId);
    }

    @Override
    public Optional<Long> getGameId() {
        return Optional.ofNullable(gameId);
    }

    @Override
    public ControllerPlayerState getState() {
        return state;
    }

    /**
     * Returns the party ID from the player.
     * If no party ID is stored, this will be null.
     * It is better to use {@link #getParty()} to check whether the player is in a party, due to additional checks.
     *
     * @return the optional with the party ID
     * @see #getParty()
     */
    @Override
    public Optional<Long> getPartyId() {
        return Optional.ofNullable(partyId);
    }

    /**
     * Sets the display name for this controller.
     *
     * @param displayName The new display name to be set.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        updateInstancePlayer();
    }

    /**
     * A dynamic way of queuing the player to join an instance or game.
     *
     * @param requestParameters The queue request parameters
     * @return the result of the queuing the request, this does not return the result of the queue
     * @throws IllegalArgumentException when the provided argument is null
     */
    public CompletableFuture<Void> queue(QueueRequestParameters requestParameters) {
        if(requestParameters == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("requestParameters must not be null"));
        }
        return queue(new QueueRequest(QueueRequestReason.PLUGIN_CONTROLLER, requestParameters));
    }

    /**
     * A dynamic way of queuing the player to join an instance or game.
     * This immediately retrieves the queue request instead of the request parameters.
     * This method is therefore intended to only be used internally,
     * as the request reason should be set by the system.
     * Please use {@link #queue(QueueRequestParameters)} if not used internally.
     *
     * @param request The queue request
     * @return The result of the queuing the request, this does not return the result of the queue
     */
    public CompletableFuture<Void> queue(QueueRequest request) { // TODO Probs not public!
        Objects.requireNonNull(request, "request must not be null");
        setQueueRequest(request); // TODO This could override an existing queue, do we want this?. Also check if this happens everywehere.
        QueueStageEvent stageEvent = new QueueStageEvent(this, request);
        handleQueueStage(stageEvent, true);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles a single state of queueing the player: it computes the result and makes sure that the result is valid.
     * For joins, it will register a slot; and if set, it will send the message/forward the player.
     * This assumes that the new state has already been added to the request.
     * This method runs several methods asynchronous, the completable future is completed when it has finished.
     * If the result is not to be handled, it will not send the party members over if needed.
     *
     * @param stageEvent the event tied to this queue request, where the result is modified of
     * @param handleResult whether this method should send the message or forward the player.
     * @return the completable future that completes when it is finished
     */
    public CompletableFuture<Void> handleQueueStage(QueueStageEvent stageEvent, boolean handleResult) { // TODO Definitely not public!
        // TODO Also do state checks: does the player already have the queue set; etc.
        Objects.requireNonNull(stageEvent, "stageEvent must not be null");

        // Loop over new stages to not use recursiveness
        QueueRequest request = stageEvent.getQueueRequest();
        Controller controller = Controller.getInstance();

        // We are now in the next stage, as we did not find a good one before, set the result to none and call the controller.
        stageEvent.setNoneResult();
        controller.handleQueueStageEvent(stageEvent);
        QueueStageEventResult result = stageEvent.getResult();
        // We have got a new result, check whether we can run on it
        return switch (result) {
            case QueueStageEventResult.None none -> {
                // We got a simple none
                if(handleResult && none.getMessage().isPresent()) {
                    controller.sendMessage(getUuid(), none.getMessage().get());
                }
                setQueueRequest(null);
                yield CompletableFuture.completedFuture(null);
            }
            case QueueStageEventResult.Disconnect disconnect -> {
                // We got a disconnect
                if (handleResult) {
                    controller.disconnectPlayer(getUuid(), disconnect.getMessage().orElse(null));
                }
                setQueueRequest(null);
                yield CompletableFuture.completedFuture(null);
            }
            case QueueStageEventResult.QueueStageEventJoinableResult joinable -> {
                // Fetch the instance and potential game; immediately check available as well; if correct, set the state
                ControllerLaneInstance resultInstance;
                Long resultGameId;
                HashSet<UUID> playTogetherPlayers = joinable.getJoinTogetherPlayers();
                int needSlots = playTogetherPlayers == null ? 1 : playTogetherPlayers.size() + 1;
                if(joinable instanceof QueueStageEventResult.JoinInstance joinInstance) {
                    // Set the IDs, and try to fetch
                    resultGameId = null;
                    Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(joinInstance.instanceId());
                    if (instanceOptional.isEmpty()) {
                        // Run the stage event again to determine a new ID.
                        request.stages().add(joinInstance.constructStage(QueueStageResult.UNKNOWN_ID, joinable.getQueueType()));
                        yield handleQueueStage(stageEvent, handleResult);
                    }
                    resultInstance = instanceOptional.get();
                    // Do availability check
                    if(!resultInstance.isQueueJoinable(joinable.getQueueType(), needSlots)) {
                        // Run the stage event again to find a joinable instance.
                        request.stages().add(joinInstance.constructStage(QueueStageResult.NOT_JOINABLE, joinable.getQueueType()));
                        yield handleQueueStage(stageEvent, handleResult);
                    }
                    setState(new ControllerPlayerState(LanePlayerState.INSTANCE_TRANSFER,
                            Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, resultInstance.getId()),
                                    new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis())))); // TODO Better state handling, probs not even cleared, etc.
                } else if(joinable instanceof QueueStageEventResult.JoinGame joinGame) {
                    // Set the IDs, and try to fetch
                    resultGameId = joinGame.gameId();
                    Optional<ControllerGame> gameOptional = controller.getGame(joinGame.gameId());
                    if (gameOptional.isEmpty()) {
                        request.stages().add(joinGame.constructStage(QueueStageResult.UNKNOWN_ID, joinable.getQueueType()));
                        yield handleQueueStage(stageEvent, handleResult);
                    }
                    ControllerGame game = gameOptional.get();
                    Optional<ControllerLaneInstance> instanceOptional = controller.getInstance(game.getInstanceId());
                    if (instanceOptional.isEmpty()) {
                        // Run the stage event again to determine a new ID.
                        request.stages().add(joinGame.constructStage(QueueStageResult.UNKNOWN_ID, joinable.getQueueType()));
                        yield handleQueueStage(stageEvent, handleResult);
                    }
                    resultInstance = instanceOptional.get();
                    // Do availability check
                    if(!resultInstance.isQueueJoinable(joinable.getQueueType(), needSlots) || !game.isQueueJoinable(joinable.getQueueType(), needSlots)) {
                        // Run the stage event again to find a joinable game.
                        request.stages().add(joinGame.constructStage(QueueStageResult.NOT_JOINABLE, joinable.getQueueType()));
                        yield handleQueueStage(stageEvent, handleResult);
                    }
                    setState(new ControllerPlayerState(LanePlayerState.GAME_TRANSFER,
                            Set.of(new ControllerStateProperty(LaneStateProperty.INSTANCE_ID, resultInstance.getId()),
                                    new ControllerStateProperty(LaneStateProperty.GAME_ID, resultGameId),
                                    new ControllerStateProperty(LaneStateProperty.TIMESTAMP, System.currentTimeMillis()))));
                } else {
                    request.stages().add(new QueueStage(QueueStageResult.INVALID_STATE, joinable.getQueueType(), null, null));
                    yield handleQueueStage(stageEvent, handleResult);
                }

                // Make the reservation
                setQueueRequest(request);
                CompletableFuture<Void> future = controller.getConnection().<Void>sendRequestPacket((id) -> new InstanceJoinPacket(id, convertRecord(), joinable.getQueueType(), resultGameId), resultInstance.getId()).getFutureResult();
                future.exceptionallyCompose(exception -> {
                    if (exception instanceof ResultUnsuccessfulException ex) {
                        // We are not allowing to join at this instance.
                        request.stages().add(new QueueStage(QueueStageResult.JOIN_DENIED, joinable.getQueueType(), resultInstance.getId(), resultGameId));
                        return handleQueueStage(stageEvent, handleResult);
                    }
                    request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, joinable.getQueueType(), resultInstance.getId(), resultGameId));
                    return handleQueueStage(stageEvent, handleResult);
                }).thenCompose(aVoid -> {
                    // We have successfully registered a slot on the server
                    if(!handleResult) return CompletableFuture.completedFuture(null);
                    return controller.joinServer(getUuid(), resultInstance.getId());
                }).exceptionallyCompose(exception -> {
                    // If handleResult is true, then we got an error
                    if (exception instanceof ResultUnsuccessfulException ex) {
                        // TODO Should we let the Instance know that the player is not joining? Maybe they claimed a spot in the queue.
                        request.stages().add(new QueueStage(QueueStageResult.SERVER_UNAVAILABLE, joinable.getQueueType(), resultInstance.getId(), resultGameId));
                        return handleQueueStage(stageEvent, handleResult);
                    }
                    request.stages().add(new QueueStage(QueueStageResult.NO_RESPONSE, joinable.getQueueType(), resultInstance.getId(), resultGameId));
                    return handleQueueStage(stageEvent, handleResult);
                }).thenAccept(aVoid -> {
                    // If handleResult is true: we have joined, send over any party members
                    if (playTogetherPlayers != null && !playTogetherPlayers.isEmpty()) {
                        QueueRequestParameter partyJoinParameter;
                        if (resultGameId != null) {
                            partyJoinParameter = QueueRequestParameter.create().gameId(resultGameId).instanceId(resultInstance.getId()).build();
                        } else {
                            partyJoinParameter = QueueRequestParameter.create().instanceId(resultInstance.getId()).build();
                        }
                        QueueRequest partyRequest = new QueueRequest(QueueRequestReason.PARTY_JOIN, QueueRequestParameters.create().add(partyJoinParameter).build());
                        playTogetherPlayers.forEach(uuid -> Controller.getPlayer(uuid).ifPresent(controllerPlayer -> controllerPlayer.queue(partyRequest)));
                    }
                });
                yield future;
            }
        };
    }

    public void setQueueRequest(QueueRequest queueRequest) { // TODO Definitely not public!
        this.queueRequest = queueRequest; // TODO This should be cleared when the request is succesfully done!
        // TODO The instance should reset this. Or we interact when we receive the updated player state.
        updateInstancePlayer();
    }

    /**
     * Sets the state of the player.
     *
     * @param state the state
     */
    public void setState(ControllerPlayerState state) {
        this.state = state; // TODO This cannot be done this easily!
        updateInstancePlayer();
    }

    /**
     * Sets the party ID of the player.
     * This can only be done if the player is correctly set in the party.
     *
     * @param partyId the party ID to set
     * @return whether the party ID has been updated with the given value
     */
    public boolean setPartyId(Long partyId) {
        if(partyId == null) {
            // We want to remove, check if we are removed if we are in a party
            Optional<ControllerParty> current = getParty();
            if(current.isEmpty() || !current.get().containsPlayer(this)) {
                // We are not in our current party
                this.partyId = partyId;
                updateInstancePlayer();
                return true;
            }
            return false;
        }
        // We want to add, check if we are in the party object
        Optional<ControllerParty> newParty = Controller.getInstance().getPartyManager().getParty(partyId);
        if(newParty.isPresent() && newParty.get().containsPlayer(this)) {
            // Party is added
            this.partyId = partyId;
            updateInstancePlayer();
            return true;
        }
        return false;
    }

    /**
     * Returns the party of the player.
     * If the player is not in a party, the optional will be empty.
     * Even if this player has a party ID, this will only return the party if it actually exists.
     *
     * @return the party
     */
    public Optional<ControllerParty> getParty() {
        return Controller.getInstance().getPartyManager().getParty(partyId);
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId; // TODO Why this easily, this should not be this easy
        updateInstancePlayer();
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId; // TODO Why this easily, this should not be this easy
        updateInstancePlayer();
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, username, displayName, queueRequest, instanceId, gameId, state.convertRecord(), partyId);
    }

    /**
     * Update this player object with the given data.
     * This should never be called after initialization.
     *
     * @param record the record with the data
     */
    @Override
    public void applyRecord(PlayerRecord record) {
        displayName = record.displayName();
        queueRequest = record.queueRequest();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if (state == null) state = new ControllerPlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

    public void updateInstancePlayer() {
        getInstanceId().ifPresent(instanceId -> Controller.getInstance().getConnection().sendPacket(new InstanceUpdatePlayerPacket(convertRecord()), instanceId));
    }

    /**
     * Retrieves the saved {@link Locale} associated with this player asynchronously.
     *
     * @return a {@link CompletableFuture} that resolves to an {@link Optional} containing the saved {@link Locale}.
     * If no locale was saved, the {@link Optional} will be empty.
     */
    public CompletableFuture<Optional<Locale>> getSavedLocale() {
        return Controller.getInstance().getPlayerManager().getSavedLocale(uuid);
    }

    /**
     * Sets the saved locale for this player asynchronously.
     *
     * @param locale The {@link Locale} to be saved for the player.
     * @return A {@link CompletableFuture} that completes when the operation finishes.
     */
    public CompletableFuture<Void> setSavedLocale(Locale locale) {
        return Controller.getInstance().getPlayerManager().setSavedLocale(uuid, locale);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ControllerPlayer.class.getSimpleName() + "[", "]").add("uuid=" + uuid).add("username='" + username + "'").add("displayName='" + displayName + "'").add("queueRequest=" + queueRequest).add("instanceId='" + instanceId + "'").add("gameId=" + gameId).add("state=" + state).add("partyId=" + partyId).toString();
    }

    // TODO Abstract sendMessage? Let VelocityController implement?

}
