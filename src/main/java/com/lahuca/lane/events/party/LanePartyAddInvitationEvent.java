package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.events.LanePartyUpdateEvent;
import com.lahuca.lane.events.LanePlayerEvent;

public abstract class LanePartyAddInvitationEvent<Y extends LaneParty, P extends LanePlayer> implements LanePartyUpdateEvent<Y>, LanePlayerEvent<P> {

    private final Y party;
    private final P invited;
    // TODO Add who/what invited

    public LanePartyAddInvitationEvent(Y party, P invited) {
        this.party = party;
        this.invited = invited;
    }

    @Override
    public Y getParty() {
        return party;
    }

    @Override
    public P getPlayer() {
        return invited;
    }

}
