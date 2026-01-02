package com.lahuca.lane.events.party;

import com.lahuca.lane.LaneParty;
import com.lahuca.lane.LanePlayer;
import com.lahuca.lane.events.LanePartyUpdateEvent;
import com.lahuca.lane.events.LanePlayerEvent;

/**
 * This event is called when a party is created.
 * In the party implementation itself, the party can also hold only just one party member.
 * Within the party command, this is expected only for public parties.
 */
public abstract class LanePartyCreateEvent<Y extends LaneParty, P extends LanePlayer> implements LanePartyUpdateEvent<Y>, LanePlayerEvent<P> {

    // TODO Add cancelled?
    private final Y party;
    private final P player;

    public LanePartyCreateEvent(Y party, P player) {
        this.party = party;
        this.player = player;
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
