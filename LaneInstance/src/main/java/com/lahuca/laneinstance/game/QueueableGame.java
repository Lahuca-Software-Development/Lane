package com.lahuca.laneinstance.game;

import com.lahuca.lane.game.Slottable;
import com.lahuca.lane.queue.QueueRequestParameter;
import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;

public interface QueueableGame extends GameLifecycle, Slottable {

    default void onGameStart() {
        setGameStarted(true);
    }

    boolean hasGameStarted();
    void setGameStarted(boolean value);

    int getNeededPlaying();
    void setNeededPlaying(int value);

    @Override
    default void onJoin(InstancePlayer instancePlayer, QueueType queueType, QueueRequestParameter parameter) {
        if(queueType == QueueType.PLAYING && getPlaying().size() >= getNeededPlaying() && !hasGameStarted()) onGameStart();
    }

    @Override
    default void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType, QueueRequestParameter parameter) {
        if(queueType == QueueType.PLAYING && getPlaying().size() >= getNeededPlaying() && !hasGameStarted()) onGameStart();
    }

}
