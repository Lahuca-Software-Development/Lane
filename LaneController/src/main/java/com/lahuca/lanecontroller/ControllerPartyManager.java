package com.lahuca.lanecontroller;

import com.lahuca.lane.data.manager.DataManager;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class ControllerPartyManager {

    private final Controller controller;
    private final DataManager dataManager;

    private final HashMap<Long, ControllerParty> parties = new HashMap<>();

    public ControllerPartyManager(Controller controller, DataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }

    public long newId() {
        long newId;
        do {
            newId = System.currentTimeMillis();
        } while (parties.containsKey(newId));
        return newId;
    }

    /**
     * Creates a party with the given owner.
     * This can only be done with the given owner is not yet in a party.
     * @param owner the owner
     * @return the party, the optional is null when the party could not be made
     * @throws IllegalArgumentException when {@code owner} is null
     */
    public Optional<ControllerParty> createParty(ControllerPlayer owner) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        if (owner.getParty().isPresent()) return Optional.empty();
        ControllerParty party = new ControllerParty(newId(), owner); // TODO We assume that we can just do this, like it should lock onto the ID.
        parties.put(party.getId(), party);
        owner.setPartyId(party.getId());
        return Optional.of(party);
    }


    /**
     * Disbands a party: removes the party object and sets the party of all players to null.
     * @param party the party to remove
     * @return {@code true} when the party has been removed, {@code false} otherwise
     * @throws IllegalArgumentException when the provided argument is null
     */
    public boolean disbandParty(ControllerParty party) {
        if(party == null) {
            throw new IllegalArgumentException("party cannot be null");
        }
        if(parties.remove(party.getId()) == null) {
            return false;
        }
        party.getPlayers().forEach(uuid ->
                controller.getPlayerManager().getPlayer(uuid).ifPresent(player ->
                        player.setPartyId(null))); // TODO What if one of them fails?
        return true;
    }


    public Set<ControllerParty> getParties() {
        return Set.copyOf(parties.values());
    }

    public Optional<ControllerParty> getParty(long id) {
        return Optional.ofNullable(parties.get(id));
    }

    public Optional<ControllerParty> getParty(ControllerPlayer player) {
        return player.getParty();
    }

}
