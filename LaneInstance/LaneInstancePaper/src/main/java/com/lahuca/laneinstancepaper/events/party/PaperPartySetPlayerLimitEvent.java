package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.events.party.PartySetInvitationsOnlyEvent;
import com.lahuca.laneinstance.events.party.PartySetPlayerLimitEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartySetPlayerLimitEvent extends PaperInstanceEvent<PartySetPlayerLimitEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartySetPlayerLimitEvent(PartySetPlayerLimitEvent instanceEvent) {
        super(true, instanceEvent);
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

    public Integer getPlayerLimit() {
        return getInstanceEvent().getPlayerLimit();
    }

    public InstanceParty getData() {
        return getInstanceEvent().getData();
    }

    public Long getReplicationId() {
        return getInstanceEvent().getReplicationId();
    }

}
