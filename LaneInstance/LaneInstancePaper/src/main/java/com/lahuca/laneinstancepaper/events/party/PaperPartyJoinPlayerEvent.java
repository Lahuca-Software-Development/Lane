package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.party.PartyDisbandEvent;
import com.lahuca.laneinstance.events.party.PartyJoinPlayerEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartyJoinPlayerEvent extends PaperInstanceEvent<PartyJoinPlayerEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartyJoinPlayerEvent(PartyJoinPlayerEvent instanceEvent) {
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

    public InstanceParty getData() {
        return getInstanceEvent().getData();
    }

    public Long getReplicationId() {
        return getInstanceEvent().getReplicationId();
    }

    public InstancePlayer getPlayer() {
        return getInstanceEvent().getPlayer();
    }

}
