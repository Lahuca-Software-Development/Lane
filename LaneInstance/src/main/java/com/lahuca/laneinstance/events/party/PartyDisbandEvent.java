package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyDisbandEvent;
import com.lahuca.laneinstance.InstanceParty;

public class PartyDisbandEvent extends LanePartyDisbandEvent<InstanceParty> {

    public PartyDisbandEvent(InstanceParty party) {
        super(party);
    }

}
