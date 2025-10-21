package com.lahuca.laneinstance;

import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.LanePlayerState;
import com.lahuca.lane.connection.packet.SendMessagePacket;
import com.lahuca.lane.data.ordered.OrderedData;
import com.lahuca.lane.data.ordered.OrderedDataComponents;
import com.lahuca.lane.data.ordered.OrderedDataMap;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.laneinstance.game.InstanceGame;
import com.lahuca.laneinstance.scoreboard.PlayerScoreboard;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author _Neko1
 * @date 14.03.2024
 **/
public class InstancePlayer implements LanePlayer {

    // Below are shared with controller
    private final UUID uuid;
    private final String username;
    private UUID networkProfileUuid;
    private String nickname;
    private QueueRequest queueRequest;
    private String instanceId = null;
    private Long gameId = null;
    private InstancePlayerState state = null;
    private Long partyId = null;
    private int queuePriority;

    // Below are instance only
    private RegisterData registerData;

    private PlayerScoreboard scoreboard;

    //TODO Add default values for the OrderedDataMap
    private final OrderedDataComponents playerListName = new OrderedDataComponents(d -> d.getPriority() > 0,
            d -> d.getPriority() == 0, d -> d.getPriority() < 0);
    private final OrderedDataComponents chatNameData = new OrderedDataComponents();

    private final OrderedDataMap<Integer> sortPriorityData = new OrderedDataMap<>();


    InstancePlayer(PlayerRecord record, RegisterData registerData) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(registerData, "registerData must not be null");
        this.uuid = record.uuid();
        this.username = record.username();
        applyRecord(record);
        this.registerData = registerData;
    }

    public record RegisterData(QueueType queueType, QueueRequestParameter parameter, Long gameId) {

        public Optional<Long> getGameId() {
            return Optional.ofNullable(gameId);
        }

    }

    /**
     * Retrieves the saved {@link Locale} associated with this player asynchronously.
     * This is done by retrieving the saved {@link Locale} at the network profile of this player.
     *
     * @return a {@link CompletableFuture} that resolves to an {@link Optional} containing the saved {@link Locale}.
     * If no locale was saved, the {@link Optional} will be empty.
     */
    public CompletableFuture<Optional<Locale>> getSavedLocale() {
        return getNetworkProfile().thenCompose(LaneInstance.getInstance().getPlayerManager()::getSavedLocale);
    }

    /**
     * Sets the saved locale for this player asynchronously.
     * This is done by setting the saved {@link Locale} at the network profile of this player.
     *
     * @param locale The {@link Locale} to be saved for the player.
     * @return A {@link CompletableFuture} that completes when the operation finishes.
     */
    public CompletableFuture<Void> setSavedLocale(Locale locale) {
        return getNetworkProfile().thenCompose(profile -> LaneInstance.getInstance().getPlayerManager().setSavedLocale(profile, locale));
    }

    public void sendMessage(Component component) {
        Objects.requireNonNull(component, "component must not be null");
        LaneInstance.getInstance().getConnection().sendPacket(new SendMessagePacket(getUuid(), component), null);
    }

    public RegisterData getRegisterData() {
        return registerData;
    }

    public void setRegisterData(RegisterData registerData) {
        Objects.requireNonNull(registerData, "registerData must not be null");
        this.registerData = registerData;
    }

    /**
     * Retrieves the player list type of the current player on the instance.
     * @return the player list type, {@link InstancePlayerListType#NONE} if not in a list
     */
    public InstancePlayerListType getInstancePlayerListType() {
        return LaneInstance.getInstance().getPlayerManager().getInstancePlayerListType(uuid);
    }

    /**
     * Retrieves the player list type of the current player on the game it is playing on.
     * @return the player list type, {@link InstancePlayerListType#NONE} if not in a list or not playing a game
     */

    public InstancePlayerListType getGamePlayerListType() {
        Optional<InstanceGame> game = getGame();
        if (game.isEmpty()) return InstancePlayerListType.NONE;
        return game.get().getGamePlayerListType(uuid);
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

    /**
     * Returns the game of the player, if it is on this instance.
     * If the player is not in a game, the optional will be empty.
     * Even if this player has a game ID, this will only return the game if it actually exists only on this instance
     *
     * @return the game
     */

    public Optional<InstanceGame> getGame() {
        return getGameId().flatMap(gameId -> LaneInstance.getInstance().getInstanceGame(gameId));
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
    public UUID getNetworkProfileUuid() {
        return networkProfileUuid;
    }

    @Override
    public CompletableFuture<InstanceProfileData> getNetworkProfile() {
        return LaneInstance.getInstance().getDataManager().getProfileData(networkProfileUuid).thenApply(opt -> opt.orElse(null));
    }

    public CompletableFuture<Void> setNetworkProfileUuid(InstanceProfileData profile) {
        return LaneInstance.getInstance().getDataManager().setNetworkProfile(this, profile);
    }

    @Override
    public Optional<String> getNickname() {
        return Optional.ofNullable(nickname);
    }

    @Override
    public CompletableFuture<Void> setNickname(String nickname) {
        return LaneInstance.getInstance().setNickname(this, nickname);
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
    public int getQueuePriority() {
        return queuePriority;
    }

    public PlayerScoreboard getScoreboard() {
        return scoreboard;
    }

    public void setScoreboard(PlayerScoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public void updatePlayerListName() {
        LaneInstance.getInstance().updatePlayerListName(uuid);
    }

    public void addPlayerListName(OrderedData<Component> data) {
        playerListName.add(data);
    }

    public void removePlayerListName(String id) {
        playerListName.remove(id);
    }

    public OrderedDataComponents getPlayerListNameData() {
        return playerListName;
    }

    public Component getChatName() {
        return getChatNameData().asComponent();
    }

    public OrderedDataComponents getChatNameData() {
        return chatNameData;
    }

    public void addSortPriority(OrderedData<Integer> data) {
        sortPriorityData.add(data);
    }

    public void removeSortPriority(String id) {
        sortPriorityData.remove(id);
    }

    public OrderedDataMap<Integer> getSortPriorityData() {
        return sortPriorityData;
    }

    @Override
    public PlayerRecord convertRecord() {
        return new PlayerRecord(uuid, username, networkProfileUuid, nickname, queueRequest, instanceId, gameId, state.convertRecord(), partyId, queuePriority);
    }

    @Override
    public void applyRecord(PlayerRecord record) {
        // TODO Maybe better recode?
        networkProfileUuid = record.networkProfileUuid();
        nickname = record.nickname();
        queueRequest = record.queueRequest();
        instanceId = record.instanceId();
        gameId = record.gameId();
        if(state == null) state = new InstancePlayerState();
        state.applyRecord(record.state());
        partyId = record.partyId();
        queuePriority = record.queuePriority();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstancePlayer.class.getSimpleName() + "[", "]")
                .add("uuid=" + uuid)
                .add("username='" + username + "'")
                .add("networkProfileUuid=" + networkProfileUuid)
                .add("nickname='" + nickname + "'")
                .add("queueRequest=" + queueRequest)
                .add("instanceId='" + instanceId + "'")
                .add("gameId=" + gameId)
                .add("state=" + state)
                .add("partyId=" + partyId)
                .add("queuePriority=" + queuePriority)
                .add("registerData=" + registerData)
                .toString();
    }

}
