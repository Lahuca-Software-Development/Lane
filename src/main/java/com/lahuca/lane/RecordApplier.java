/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 19-3-2024 at 20:46 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane;

public interface RecordApplier<T extends Record> {

	/**
	 * Convert the object to a record with the correct data.
	 * @return the record
	 */
	T convertRecord();

	/**
	 * Applies the given record to the object. The object will have copied over the values.
	 * @param record the record
	 */
	void applyRecord(T record);

}
