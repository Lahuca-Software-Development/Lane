package com.lahuca.laneinstancepaper.events.party;

import com.lahuca.lane.queue.QueueType;
import com.lahuca.laneinstance.InstanceParty;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.events.InstanceJoinEvent;
import com.lahuca.laneinstance.events.party.PartyAcceptInvitationEvent;
import com.lahuca.laneinstancepaper.events.PaperInstanceEvent;
import org.bukkit.event.HandlerList;

public class PaperPartyAcceptInvitationEvent extends PaperInstanceEvent<PartyAcceptInvitationEvent> {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PaperPartyAcceptInvitationEvent(PartyAcceptInvitationEvent instanceEvent) {
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
