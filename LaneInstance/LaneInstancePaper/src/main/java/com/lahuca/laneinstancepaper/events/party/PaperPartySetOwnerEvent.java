package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.party.PartySetInvitationsOnlyEvent;
import com.lahuca.laneinstance.events.party.PartySetOwnerEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartySetOwnerEvent extends PaperInstanceEvent<PartySetOwnerEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartySetOwnerEvent(PartySetOwnerEvent instanceEvent) {
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

    public InstancePlayer getPlayer() {
        return getInstanceEvent().getPlayer();
    }

    public InstanceParty getData() {
        return getInstanceEvent().getData();
    }

    public Long getReplicationId() {
        return getInstanceEvent().getReplicationId();
    }

}
