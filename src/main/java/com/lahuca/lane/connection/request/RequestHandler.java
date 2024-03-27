/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 22-3-2024 at 21:19 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.connection.request;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class RequestHandler {

    private final HashMap<Long, CompletableFuture<Result<?>>> requests = new HashMap<>(); // TODO Make cachable, for lost requests

    // TODO Do the request function in here.

    /**
     * Build request future, which will also be added to the requests.
     * The object of the result will be cast to the correct type as provided by the given function.
     * @param id the request ID
     * @param converter the converter that maps immediately casts any object to the given type
     * @return the return completable future with the result
     * @param <T> the object type to convert to
     */
    protected <T> CompletableFuture<Result<T>> buildFuture(long id, Function<Result<?>, Result<T>> converter) {
        CompletableFuture<Result<?>> future = new CompletableFuture<>();
        requests.put(id, future);
        return future.thenApply(converter);
    }

    /**
     * Build request future, which will also be added to the requests.
     * The result only consists of the result state not any data
     * @param id the request ID
     * @return the return completable future
     */
    protected CompletableFuture<Result<Void>> buildVoidFuture(long id) {
        return buildFuture(id, result -> new Result<>(result.result()));
    }

    protected long getNewRequestId() {
        long id;
        do {
            id = System.currentTimeMillis();
        } while(requests.containsKey(id));
        return id;
    }

    protected HashMap<Long, CompletableFuture<Result<?>>> getRequests() {
        return requests;
    }

    protected Result<Void> simpleResult(String result) {
        return new Result<>(result);
    }

    protected CompletableFuture<Result<Void>> simpleFuture(String result) {
        return CompletableFuture.completedFuture(new Result<>(result));
    }

}
