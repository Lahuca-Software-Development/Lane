/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:23 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

import com.lahuca.lane.data.DataObjectId;
import com.lahuca.lane.data.RelationalId;
import com.lahuca.lane.data.profile.ProfileData;
import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RecordConverterApplier;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LanePlayer extends RecordConverterApplier<PlayerRecord> {

	UUID getUuid();
	String getUsername();

	UUID getNetworkProfileUuid();
    CompletableFuture<? extends ProfileData> getNetworkProfile();

    /**
     * Retrieves sub profiles with the given name and active state from the network profile.
     * If none exists, one will be created
     *
     * @param name   the name
     * @param active the active state
     * @return a {@link CompletableFuture} with the sub profile ID
     */
    default CompletableFuture<Set<UUID>> fetchSubProfilesIds(String name, boolean active) {
        return getNetworkProfile().thenCompose(networkProfile -> networkProfile.fetchSubProfilesIds(name, active));
    }
	default DataObjectId getNetworkProfileDataObjectId(String id) {
		return new DataObjectId(RelationalId.Profiles(getNetworkProfileUuid()), id);
	}

	Optional<String> getNickname();
	Optional<QueueRequest> getQueueRequest();
	Optional<String> getInstanceId();
	Optional<Long> getGameId();
	LanePlayerState getState();
	Optional<Long> getPartyId();
    int getQueuePriority();

}
