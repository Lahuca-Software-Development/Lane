package com.lahuca.lane.connection.packet;

import com.lahuca.lane.connection.Packet;
import com.lahuca.lane.connection.packet.replicated.*;
import com.lahuca.lane.connection.request.RequestPacket;
import com.lahuca.lane.connection.request.ResponsePacket;
import com.lahuca.lane.records.PartyRecord;

import java.util.UUID;

/**
 * This class holds the packets for transmitting information and operations between the controller and instance about parties.
 *
 * @author _Neko1
 * @date 19.03.2024
 **/
public interface PartyPacket {

    static void register() {
        Packet.registerPacket(Event.AcceptInvitation.packetId, Event.AcceptInvitation.class);
        Packet.registerPacket(Event.AddInvitation.packetId, Event.AddInvitation.class);
        Packet.registerPacket(Event.Create.packetId, Event.Create.class);
        Packet.registerPacket(Event.DenyInvitation.packetId, Event.DenyInvitation.class);
        Packet.registerPacket(Event.Disband.packetId, Event.Disband.class);
        Packet.registerPacket(Event.JoinPlayer.packetId, Event.JoinPlayer.class);
        Packet.registerPacket(Event.RemovePlayer.packetId, Event.RemovePlayer.class);
        Packet.registerPacket(Event.SetInvitationsOnly.packetId, Event.SetInvitationsOnly.class);
        Packet.registerPacket(Event.SetOwner.packetId, Event.SetOwner.class);
        Packet.registerPacket(Event.Warp.packetId, Event.Warp.class);

        Packet.registerPacket(Retrieve.Request.packetId, Retrieve.Request.class);
        Packet.registerPacket(Retrieve.RequestPlayerParty.packetId, Retrieve.RequestPlayerParty.class);
        Packet.registerPacket(Retrieve.Response.packetId, Retrieve.Response.class);
        Packet.registerPacket(Retrieve.Subscribe.packetId, Retrieve.Subscribe.class);
        Packet.registerPacket(Retrieve.Unsubscribe.packetId, Retrieve.Unsubscribe.class);

        Packet.registerPacket(Operations.AcceptInvitation.packetId, Operations.AcceptInvitation.class);
        Packet.registerPacket(Operations.AddInvitation.packetId, Operations.AddInvitation.class);
        Packet.registerPacket(Operations.Create.packetId, Operations.Create.class);
        Packet.registerPacket(Operations.DenyInvitation.packetId, Operations.DenyInvitation.class);
        Packet.registerPacket(Operations.Disband.packetId, Operations.Disband.class);
        Packet.registerPacket(Operations.JoinPlayer.packetId, Operations.JoinPlayer.class);
        Packet.registerPacket(Operations.RemovePlayer.packetId, Operations.RemovePlayer.class);
        Packet.registerPacket(Operations.SetInvitationsOnly.packetId, Operations.SetInvitationsOnly.class);
        Packet.registerPacket(Operations.SetOwner.packetId, Operations.SetOwner.class);
        Packet.registerPacket(Operations.Warp.packetId, Operations.Warp.class);
    }

    // Helpers
    interface PartyReplicatedPacket extends PartyPacket, ReplicatedPacket<Long> {

        long partyId();

        @Override
        default Long getReplicationId() {
            return partyId();
        }

    }
    interface PartyUpdatePacket extends PartyReplicatedPacket, ReplicatedUpdatePacket<Long, PartyRecord> { }

    /**
     * The events in this interface are the events sent from the controller to the instance to update the party.
     */
    interface Event extends PartyReplicatedPacket {

        record AcceptInvitation(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventAcceptInvitation";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record AddInvitation(long partyId, UUID invited, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventAddInvitation";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record Create(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventCreate";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record DenyInvitation(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventDenyInvitation";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record Disband(long partyId) implements Packet, Event, ReplicatedRemovePacket<Long> {

            public static final String packetId = "partyEventDisband";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record JoinPlayer(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventJoinPlayer";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record RemovePlayer(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventRemovePlayer";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record SetInvitationsOnly(long partyId, boolean invitationsOnly, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventSetInvitationsOnly";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record SetOwner(long partyId, UUID player, PartyRecord value) implements Packet, Event, PartyUpdatePacket {

            public static final String packetId = "partyEventSetOwner";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        record Warp(long partyId) implements Packet, Event {

            public static final String packetId = "partyEventWarp";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

    }

    /**
     * These packets are used to transfer party packets over.
     */
    interface Retrieve extends PartyPacket {

        /**
         * Packet for retrieving a party.
         * From instance: request to retrieve.
         * @param requestId the request ID
         * @param partyId the party ID
         */
        record Request(long requestId, long partyId) implements RequestPacket, Retrieve, PartyReplicatedPacket {

            public static final String packetId = "partyRetrieveRequest";

            static {
                Packet.registerPacket(packetId, Request.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public long getRequestId() {
                return requestId;
            }

        }

        /**
         * Packet for retrieving a party by player UUID.
         * From instance: request to retrieve.
         * @param requestId the request ID
         * @param player the player UUID
         * @param createIfNeeded whether to create a party if it does not exist
         */
        record RequestPlayerParty(long requestId, UUID player, boolean createIfNeeded) implements RequestPacket, Retrieve {

            public static final String packetId = "partyPlayerRetrieveRequest";

            static {
                Packet.registerPacket(packetId, RequestPlayerParty.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public long getRequestId() {
                return requestId;
            }

        }

        /**
         * Packet for retrieving a party.
         * From controller: response to retrieve.
         * @param requestId the request ID
         * @param result the result of the operation
         * @param value the party record
         */
        record Response(long requestId, String result,
                        PartyRecord value) implements ResponsePacket<PartyRecord>, Retrieve, PartyUpdatePacket {

            public static final String packetId = "partyRetrieveResponse";

            static {
                Packet.registerPacket(packetId, Response.class);
            }

            public Response(long requestId, String result) {
                this(requestId, result, null);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public long getRequestId() {
                return requestId;
            }

            @Override
            public String getResult() {
                return result;
            }

            @Override
            public PartyRecord getData() {
                return value;
            }

            @Override
            public long partyId() {
                return value.partyId();
            }

        }

        /**
         * Packet for subscribing to a party.
         * Only from the instance.
         * @param partyId the party ID
         */
        record Subscribe(
                long partyId) implements Packet, Retrieve, PartyReplicatedPacket, ReplicatedSubscribePacket<Long> {

            public static final String packetId = "partyRetrieveSubscribe";

            static {
                Packet.registerPacket(packetId, Subscribe.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public Long getReplicationId() {
                return partyId;
            }

        }

        /**
         * Packet for unsubscribing to a party.
         * Only from the instance.
         * @param partyId the party ID
         */
        record Unsubscribe(
                long partyId) implements Packet, PartyReplicatedPacket, ReplicatedUnsubscribePacket<Long> {

            public static final String packetId = "partyRetrieveUnsubscribe";

            static {
                Packet.registerPacket(packetId, Unsubscribe.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

            @Override
            public Long getReplicationId() {
                return partyId;
            }

        }

    }

    /**
     * These events are sent from the instance to apply some operations to the party.
     * This is solely the request to them, the Controller handles the events and the operations.
     */
    interface Operations extends PartyPacket {

        /**
         * Packet for accepting an invitation of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to accept
         */
        record AcceptInvitation(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsAcceptInvitation";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for invitation of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to invite
         */
        record AddInvitation(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsAddInvitation";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for creating a party.
         * @param requestId the request ID
         * @param player the player/owner to create the party for
         */
        record Create(long requestId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsCreate";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for denying an invitation of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to deny
         */
        record DenyInvitation(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsDenyInvitation";


            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for disbanding a party.
         * @param requestId the request ID
         * @param partyId the party ID
         */
        record Disband(long requestId, long partyId) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsDisband";

            static {
                Packet.registerPacket(packetId, Disband.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for joining a player of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to join
         */
        record JoinPlayer(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsJoinPlayer";

            static {
                Packet.registerPacket(packetId, JoinPlayer.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for removing a player of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to remove
         */
        record RemovePlayer(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsRemovePlayer";

            static {
                Packet.registerPacket(packetId, RemovePlayer.class);
            }

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for setting invitations only of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param invitationsOnly the value to set
         */
        record SetInvitationsOnly(long requestId, long partyId, boolean invitationsOnly) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsSetInvitationsOnly";


            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for setting the owner of a party.
         * @param requestId the request ID
         * @param partyId the party ID
         * @param player the player to set as owner
         */
        record SetOwner(long requestId, long partyId, UUID player) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsSetOwner";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

        /**
         * Packet for warping a party.
         * @param requestId the request ID
         * @param partyId the party ID
         */
        record Warp(long requestId, long partyId) implements RequestPacket, Operations {

            public static final String packetId = "partyOperationsWarp";

            @Override
            public String getPacketId() {
                return packetId;
            }

        }

    }

}
