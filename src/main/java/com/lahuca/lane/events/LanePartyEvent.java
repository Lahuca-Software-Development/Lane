package com.lahuca.lane.events;

import com.lahuca.lane.LaneParty;

public interface LanePartyEvent<Y extends LaneParty> extends LaneEvent, LaneReplicatedEvent<Long> {

    Y getParty();

    @Override
    default Long getReplicationId() {
        return getParty().getId();
    }

}
