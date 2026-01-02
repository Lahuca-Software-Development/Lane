package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyWarpEvent;
import com.lahuca.laneinstance.InstanceParty;

public class PartyWarpEvent extends LanePartyWarpEvent<InstanceParty> {

    public PartyWarpEvent(InstanceParty party) {
        super(party);
    }

}
