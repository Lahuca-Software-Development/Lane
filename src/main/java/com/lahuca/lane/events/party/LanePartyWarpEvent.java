package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.events.LanePartyEvent;

public abstract class LanePartyWarpEvent<Y extends LaneParty> implements LanePartyEvent<Y> {

    private final Y party;

    public LanePartyWarpEvent(Y party) {
        this.party = party;
    }

    @Override
    public Y getParty() {
        return party;
    }

}
