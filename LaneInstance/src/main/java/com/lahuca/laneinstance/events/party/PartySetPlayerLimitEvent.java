package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartySetInvitationsOnlyEvent;
import com.lahuca.lane.events.party.LanePartySetPlayerLimitEvent;
import com.lahuca.laneinstance.InstanceParty;

public class PartySetPlayerLimitEvent extends LanePartySetPlayerLimitEvent<InstanceParty> {

    public PartySetPlayerLimitEvent(InstanceParty party, Integer playerLimit) {
        super(party, playerLimit);
    }

}
