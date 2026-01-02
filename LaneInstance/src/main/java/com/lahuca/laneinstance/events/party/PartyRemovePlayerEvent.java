package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyRemovePlayerEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyRemovePlayerEvent extends LanePartyRemovePlayerEvent<InstanceParty, InstancePlayer> {

    public PartyRemovePlayerEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
