package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.party.PartyRemovePlayerEvent;
import com.lahuca.laneinstance.events.party.PartySetInvitationsOnlyEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartySetInvitationsOnlyEvent extends PaperInstanceEvent<PartySetInvitationsOnlyEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartySetInvitationsOnlyEvent(PartySetInvitationsOnlyEvent instanceEvent) {
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

    public boolean isInvitationsOnly() {
        return getInstanceEvent().isInvitationsOnly();
    }

    public InstanceParty getData() {
        return getInstanceEvent().getData();
    }

    public Long getReplicationId() {
        return getInstanceEvent().getReplicationId();
    }

}
