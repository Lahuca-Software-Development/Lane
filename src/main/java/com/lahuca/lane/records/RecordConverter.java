package com.lahuca.lane.records;

public interface RecordConverter<T extends Record> {

    /**
     * Convert the object to a record with the correct data.
     *
     * @return the record
     */
    T convertRecord();

}
