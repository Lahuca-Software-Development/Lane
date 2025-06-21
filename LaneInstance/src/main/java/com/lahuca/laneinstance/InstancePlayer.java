package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.connection.request.Request;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.records.PlayerRecord;

import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstancePlayer implements LanePlayer {


    private final UUID uuid;
    private final String username;
    private String displayName;
    private QueueRequest queueRequest;
    private String instanceId = null;
    private Long gameId = null;
    private InstancePlayerState state = null;
    private Long partyId = null;

    InstancePlayer(PlayerRecord record) {
        this.uuid = record.uuid();
        this.username = record.username();
        applyRecord(record);
    }

    /**
     * Retrieves the saved locale associated with this player.
     *
     * @return a {@code Request} containing an {@code Optional} of {@code Locale} representing the saved locale,
     *         or an empty {@code Optional} if no locale is saved.
     */
    public Request<Optional<Locale>> getSavedLocale() {
        return LaneInstance.getInstance().getPlayerManager().getSavedLocale(uuid);
    }

    /**
     * Sets the saved locale for this player.
     *
     * @param locale the {@code Locale} to be set as the saved locale for this player.
     * @return a {@code Request<Void>} representing the asynchronous operation for setting the saved locale.
     */
    public Request<Void> setSavedLocale(Locale locale) {
        return LaneInstance.getInstance().getPlayerManager().setSavedLocale(uuid, locale);
    }

    @Override
    public Optional<QueueRequest> getQueueRequest() {
        return Optional.ofNullable(queueRequest);
    }

    @Override
    public Optional<String> getInstanceId() {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getGameId() {
        return Optional.ofNullable(gameId);
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
    public LanePlayerState getState() {
        return state;
    }

    @Override
    public Optional<Long> getPartyId() {
        return Optional.ofNullable(partyId);
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, username, displayName, queueRequest, instanceId, gameId, state.convertRecord(), partyId);
    }

    @Override
    public void applyRecord(PlayerRecord record) {
        // TODO Maybe better recode?
        displayName = record.displayName();
        queueRequest = record.queueRequest();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new InstancePlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstancePlayer.class.getSimpleName() + "[", "]").add("uuid=" + uuid).add("username='" + username + "'").add("displayName='" + displayName + "'").add("queueRequest=" + queueRequest).add("instanceId='" + instanceId + "'").add("gameId=" + gameId).add("state=" + state).add("partyId=" + partyId).toString();
    }

}
