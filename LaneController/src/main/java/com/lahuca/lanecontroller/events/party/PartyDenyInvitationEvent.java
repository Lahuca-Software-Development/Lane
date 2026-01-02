package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyDenyInvitationEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyDenyInvitationEvent extends LanePartyDenyInvitationEvent<ControllerParty, ControllerPlayer> {

    public PartyDenyInvitationEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
