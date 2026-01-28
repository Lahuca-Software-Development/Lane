/**
 * The classes in this package are used to convert the retrieved results correctly.
 * Due to the data transfer, GSON does not correctly convert all data types correctly without more information about the provided types.
 * These are solely used to transfer the data, the actual requests fill in the data in a {@link java.util.concurrent.CompletableFuture} if completed;
 * or will throw a {@link com.lahuca.lane.connection.request.ResponseErrorException} in the {@link java.util.concurrent.CompletableFuture} if failed.
 */
package com.lahuca.lane.connection.request.result;