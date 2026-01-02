package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartyCreateEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartyCreateEvent extends LanePartyCreateEvent<ControllerParty, ControllerPlayer> {

    public PartyCreateEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
