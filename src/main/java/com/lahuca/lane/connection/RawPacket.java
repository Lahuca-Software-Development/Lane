/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 24-2-2025 at 23:50 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2025
 */
package com.lahuca.lane.connection;

/**
 * A packet with the tied type name and its raw JSON data.
 * This is given when the connection could not properly cast the type of the packet to its record.
 * This packet ID from {@link #getPacketId()} always returns the value of the type.
 * This packet must never be registered, so that no by accident this packet is casted.
 * @param type The type of the packet.
 * @param data The raw JSON data.
 */
public record RawPacket(String type, String data) implements Packet {

    @Override
    public String getPacketId() {
        return type;
    }

}
