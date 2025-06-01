package com.lahuca.lanecontroller.managers;

import com.lahuca.lane.data.manager.DataManager;
import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ControllerPartyManager {

    private final Controller controller;
    private final DataManager dataManager;

    private final HashMap<Long, ControllerParty> parties = new HashMap<>();

    public ControllerPartyManager(Controller controller, DataManager dataManager) {
        this.controller = controller;
        this.dataManager = dataManager;
    }

    /**
     * @param owner
     * @param invited
     */
    public void createParty(ControllerPlayer owner, ControllerPlayer invited) {
        if (owner == null || invited == null) return;
        if (owner.getPartyId().isPresent()) return;
        ControllerParty controllerParty = new ControllerParty(System.currentTimeMillis(), owner.getUuid());
        // TODO Check ID for doubles

        controllerParty.sendRequest(invited);
    }

    public void disbandParty(ControllerParty party) { // TODO Redo: might send packets to servers with party info
        if (!parties.containsKey(party.getId())) return;
        parties.remove(party.getId());

        for (UUID uuid : party.getPlayers()) {
            controller.getPlayer(uuid).ifPresent(player -> player.setParty(null));
        }

        party.disband();
    }

    public Collection<ControllerParty> getParties() {
        return parties.values();
    } // TODO Redo

    public Optional<ControllerParty> getParty(long id) {
        return Optional.ofNullable(parties.get(id));
    } // TODO Redo

}
