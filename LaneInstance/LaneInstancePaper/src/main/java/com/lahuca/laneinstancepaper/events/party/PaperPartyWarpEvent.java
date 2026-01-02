package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.party.PartySetOwnerEvent;
import com.lahuca.laneinstance.events.party.PartyWarpEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartyWarpEvent extends PaperInstanceEvent<PartyWarpEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartyWarpEvent(PartyWarpEvent instanceEvent) {
        super(instanceEvent);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }


    public InstanceParty getParty() {
        return getInstanceEvent().getParty();
    }

    public Long getReplicationId() {
        return getInstanceEvent().getReplicationId();
    }

}
