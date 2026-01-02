package com.lahuca.laneinstance.events.party;

import com.lahuca.lane.events.party.LanePartyJoinPlayerEvent;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;

public class PartyJoinPlayerEvent extends LanePartyJoinPlayerEvent<InstanceParty, InstancePlayer> {

    public PartyJoinPlayerEvent(InstanceParty party, InstancePlayer player) {
        super(party, player);
    }

}
