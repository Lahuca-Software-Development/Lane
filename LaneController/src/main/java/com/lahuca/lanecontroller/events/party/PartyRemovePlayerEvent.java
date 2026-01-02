package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyRemovePlayerEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyRemovePlayerEvent extends LanePartyRemovePlayerEvent<ControllerParty, ControllerPlayer> {

    public PartyRemovePlayerEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
