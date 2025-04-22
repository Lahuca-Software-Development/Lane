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

import com.lahuca.lane.queue.QueueRequest;
import com.lahuca.lane.records.PlayerRecord;
import com.lahuca.lane.records.RecordConverterApplier;

import java.util.Optional;
import java.util.UUID;

public interface LanePlayer extends RecordConverterApplier<PlayerRecord> {

	UUID getUuid();
	String getUsername();
	String getDisplayName();
	Optional<QueueRequest> getQueueRequest();
	Optional<String> getInstanceId();
	Optional<Long> getGameId();
	LanePlayerState getState();
	Optional<Long> getPartyId();

}
