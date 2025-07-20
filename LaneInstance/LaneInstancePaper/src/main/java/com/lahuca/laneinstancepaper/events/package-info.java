/**
 * The events in this package reimplement the instance events on Paper.
 * This is due to the event system of BukkitMC, so that an event should implement {@link org.bukkit.event.Event}.
 * The classes extend the abstract class of {@link com.lahuca.laneinstancepaper.events.PaperInstanceEvent},
 * this has the actual Instance event in a field.
 * The implemented classes then delegate the methods from that field so that everything is handled as normally.
 */
package com.lahuca.laneinstancepaper.events;