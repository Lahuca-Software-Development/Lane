package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.events.LanePartyUpdateEvent;

public abstract class LanePartySetPlayerLimitEvent<Y extends LaneParty> implements LanePartyUpdateEvent<Y> {

    private final Y party;
    private final Integer playerLimit;

    public LanePartySetPlayerLimitEvent(Y party, Integer playerLimit) {
        this.party = party;
        this.playerLimit = playerLimit;
    }

    @Override
    public Y getParty() {
        return party;
    }

    public Integer getPlayerLimit() {
        return playerLimit;
    }

}
