package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.events.LanePartyUpdateEvent;
import com.lahuca.lane.events.LanePlayerEvent;

public abstract class LanePartyAcceptInvitationEvent<Y extends LaneParty, P extends LanePlayer> implements LanePartyUpdateEvent<Y>, LanePlayerEvent<P> {

    private final Y party;
    private final P player;

    public LanePartyAcceptInvitationEvent(Y party, P player) {
        this.player = player;
        this.party = party;
    }

    @Override
    public Y getParty() {
        return party;
    }

    @Override
    public P getPlayer() {
        return player;
    }

}
