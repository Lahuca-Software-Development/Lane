package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyAcceptInvitationEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyAcceptInvitationEvent extends LanePartyAcceptInvitationEvent<InstanceParty, InstancePlayer> {

    public PartyAcceptInvitationEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
