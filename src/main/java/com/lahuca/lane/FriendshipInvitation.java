package com.lahuca.lane;

import com.lahuca.lane.records.RecordConverter;
import com.lahuca.lane.records.RelationshipRecord;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record FriendshipInvitation(UUID requester, UUID invited) implements RecordConverter<RelationshipRecord> {

    public FriendshipInvitation {
        Objects.requireNonNull(requester, "requester cannot be null");
        Objects.requireNonNull(invited, "invited cannot be null");
    }

    @Override
    public RelationshipRecord convertRecord() {
        return new RelationshipRecord(null, Set.of(requester, invited));
    }

}