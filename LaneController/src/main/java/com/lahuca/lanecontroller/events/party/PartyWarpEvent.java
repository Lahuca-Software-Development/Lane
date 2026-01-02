package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyWarpEvent;
import com.lahuca.lanecontroller.ControllerParty;

public class PartyWarpEvent extends LanePartyWarpEvent<ControllerParty> {

    public PartyWarpEvent(ControllerParty party) {
        super(party);
    }

}
