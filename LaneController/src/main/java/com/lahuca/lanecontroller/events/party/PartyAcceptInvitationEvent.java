package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyAcceptInvitationEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyAcceptInvitationEvent extends LanePartyAcceptInvitationEvent<ControllerParty, ControllerPlayer> {

    public PartyAcceptInvitationEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
