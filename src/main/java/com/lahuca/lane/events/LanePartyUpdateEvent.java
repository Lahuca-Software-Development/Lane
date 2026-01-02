package com.lahuca.lane.events;

import com.lahuca.lane.LaneParty;

/**
 * An event that is called when a party's data is updated.
 * @param <Y> The party class type.
 */
public interface LanePartyUpdateEvent<Y extends LaneParty> extends LanePartyEvent<Y>, LaneReplicatedUpdateEvent<Long, Y> {

    Y getParty();

    @Override
    default Y getData() {
        return getParty();
    }

    @Override
    default Long getReplicationId() {
        return getParty().getId();
    }

}
