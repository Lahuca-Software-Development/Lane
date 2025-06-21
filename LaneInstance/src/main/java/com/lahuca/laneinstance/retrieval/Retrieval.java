package com.lahuca.laneinstance.retrieval;

public interface Retrieval {

    // TODO Maybe add a method to like set whether the retrieval should update every X seconds or so.

    /**
     * Returns the timestamp of retrieved data where it was lastly fully retrieved.
     * @return the timestamp
     */
    long getRetrievalTimestamp();

}
