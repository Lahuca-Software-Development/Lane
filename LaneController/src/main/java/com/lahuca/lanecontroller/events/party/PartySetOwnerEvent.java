package com.lahuca.lanecontroller.events.party;

import com.lahuca.lane.events.party.LanePartySetOwnerEvent;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

public class PartySetOwnerEvent extends LanePartySetOwnerEvent<ControllerParty, ControllerPlayer> {

    public PartySetOwnerEvent(ControllerParty party, ControllerPlayer player) {
        super(party, player);
    }

}
