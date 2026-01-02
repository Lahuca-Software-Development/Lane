package com.lahuca.lane.events;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;

public interface LanePlayerEvent<P extends LanePlayer> extends LaneEvent {

    P getPlayer();

}
