package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartySetInvitationsOnlyEvent;
import com.lahuca.lane.events.party.LanePartySetPlayerLimitEvent;
import com.lahuca.lanecontroller.ControllerParty;

public class PartySetPlayerLimitEvent extends LanePartySetPlayerLimitEvent<ControllerParty> {

    public PartySetPlayerLimitEvent(ControllerParty party, Integer playerLimit) {
        super(party, playerLimit);
    }

}
