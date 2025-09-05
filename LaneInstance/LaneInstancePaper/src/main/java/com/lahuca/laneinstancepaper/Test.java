package com.lahuca.laneinstancepaper;

import com.lahuca.lane.data.profile.ProfileType;
import com.lahuca.laneinstance.InstancePlayer;
import com.lahuca.laneinstance.InstanceProfileData;
import com.lahuca.laneinstance.LaneInstance;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Test {

    static {
        InstancePlayer player = LaneInstance.getInstance().getPlayerManager().getInstancePlayer(UUID.randomUUID()).get();
        String profileId = "practice";
        InstanceProfileData subProfile = player.getNetworkProfile().thenCompose(profile -> {
            HashSet<UUID> subProfiles = profile.getSubProfiles(profileId, true);
            if(subProfiles.isEmpty()) {
                return profile.createSubProfile(ProfileType.SUB, profileId, true);
            }
            return CompletableFuture.completedFuture(subProfiles.iterator().next());
        }).join();
    }

}
