package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyDenyInvitationEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyDenyInvitationEvent extends LanePartyDenyInvitationEvent<InstanceParty, InstancePlayer> {

    public PartyDenyInvitationEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
