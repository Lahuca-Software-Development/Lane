package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyJoinPlayerEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyJoinPlayerEvent extends LanePartyJoinPlayerEvent<ControllerParty, ControllerPlayer> {

    public PartyJoinPlayerEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
