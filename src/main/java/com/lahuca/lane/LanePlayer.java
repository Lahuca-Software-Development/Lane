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

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LanePlayer extends RecordApplier<PlayerRecord> {

	UUID getUuid();
	String getName();
	String getDisplayName();
	Locale getLanguage();
	Optional<QueueRequest> getQueueRequest();
	Optional<String> getInstanceId();
	Optional<Long> getGameId();
	LanePlayerState getState();
	Optional<Long> getPartyId();
	Set<Long> getRelationships();
}
