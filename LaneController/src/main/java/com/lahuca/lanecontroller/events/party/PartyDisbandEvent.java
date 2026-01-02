package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyDisbandEvent;
import com.lahuca.lanecontroller.ControllerParty;

public class PartyDisbandEvent extends LanePartyDisbandEvent<ControllerParty> {

    public PartyDisbandEvent(ControllerParty party) {
        super(party);
    }

}
