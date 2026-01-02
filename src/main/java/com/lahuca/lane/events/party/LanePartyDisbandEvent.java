package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.events.LanePartyEvent;
import com.lahuca.lane.events.LaneReplicatedRemoveEvent;

public abstract class LanePartyDisbandEvent<Y extends LaneParty> implements LanePartyEvent<Y>, LaneReplicatedRemoveEvent<Long> {

    private final Y party;

    public LanePartyDisbandEvent(Y party) {
        this.party = party;
    }

    @Override
    public Y getParty() {
        return party;
    }

}
