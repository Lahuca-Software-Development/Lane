package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyAddInvitationEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyAddInvitationEvent extends LanePartyAddInvitationEvent<ControllerParty, ControllerPlayer> {

    public PartyAddInvitationEvent(ControllerParty party, ControllerPlayer invited) {
        super(party, invited);
    }

}
