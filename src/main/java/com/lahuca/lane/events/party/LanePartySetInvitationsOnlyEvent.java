package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.events.LanePartyUpdateEvent;

public abstract class LanePartySetInvitationsOnlyEvent<Y extends LaneParty> implements LanePartyUpdateEvent<Y> {

    private final Y party;
    private final boolean invitationsOnly;

    public LanePartySetInvitationsOnlyEvent(Y party, boolean invitationsOnly) {
        this.party = party;
        this.invitationsOnly = invitationsOnly;
    }

    @Override
    public Y getParty() {
        return party;
    }

    public boolean isInvitationsOnly() {
        return invitationsOnly;
    }

}
