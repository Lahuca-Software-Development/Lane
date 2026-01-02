package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartySetInvitationsOnlyEvent;
import com.lahuca.laneinstance.InstanceParty;

public class PartySetInvitationsOnlyEvent extends LanePartySetInvitationsOnlyEvent<InstanceParty> {

    public PartySetInvitationsOnlyEvent(InstanceParty party, boolean invitationsOnly) {
        super(party, invitationsOnly);
    }

}
