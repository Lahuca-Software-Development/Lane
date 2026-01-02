package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartySetInvitationsOnlyEvent;
import com.lahuca.lanecontroller.ControllerParty;

public class PartySetInvitationsOnlyEvent extends LanePartySetInvitationsOnlyEvent<ControllerParty> {

    public PartySetInvitationsOnlyEvent(ControllerParty party, boolean invitationsOnly) {
        super(party, invitationsOnly);
    }

}
