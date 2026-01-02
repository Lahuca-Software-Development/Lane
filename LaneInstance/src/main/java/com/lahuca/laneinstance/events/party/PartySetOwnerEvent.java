package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartySetOwnerEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartySetOwnerEvent extends LanePartySetOwnerEvent<InstanceParty, InstancePlayer> {

    public PartySetOwnerEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
