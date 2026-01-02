package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyCreateEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyCreateEvent extends LanePartyCreateEvent<InstanceParty, InstancePlayer> {

    public PartyCreateEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
