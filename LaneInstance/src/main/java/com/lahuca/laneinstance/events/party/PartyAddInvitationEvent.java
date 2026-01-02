package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyAddInvitationEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyAddInvitationEvent extends LanePartyAddInvitationEvent<InstanceParty, InstancePlayer> {

    public PartyAddInvitationEvent(InstanceParty party, InstancePlayer invited) {
        super(party, invited);
    }

}
