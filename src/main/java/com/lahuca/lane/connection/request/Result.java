/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 25-3-2024 at 15:12 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.request;

public record Result<T>(String result, T data) {

    public Result(String result) {
        this(result, null);
    }

    public Result(ResponsePacket<T> response) {
        this(response.getResult(), response.getData());
    }

    public boolean isSuccessful() {
        return result.equals(ResponsePacket.OK);
    }

}
