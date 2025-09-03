package com.lahuca.laneinstance.game;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstancePlayerListType;

public interface GameLifecycle {

    void onStartup();
    void onShutdown();
    void onJoin(InstancePlayer instancePlayer, QueueType queueType);
    void onQuit(InstancePlayer instancePlayer);
    void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType);

}
