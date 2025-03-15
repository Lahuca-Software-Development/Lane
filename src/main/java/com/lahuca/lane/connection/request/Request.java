/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 25-2-2025 at 23:26 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2025
 */
package com.lahuca.lane.connection.request;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A request is defined by a packet that is expected for which a response is needed.
 * A request and its response are combined using request IDs.
 * When a request is sent over a connection, its response is to be expected to be cast to a {@link Result} object.
 * This {@link Result} object contains the response from the other side of the connection:
 * the status of the result and if present an object that is requested.
 * The {@link CompletableFuture} with a wrapped {@link Result} object handles the asynchronous response.
 * This {@link CompletableFuture} is canceled when no response is received within a set timeframe: the timeout.
 * As results are frequent of the generic unknown type ?, the result parser is to be used to properly parse the result before sending the future.
 * @param <T> The result type.
 */
public class Request<T> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 3;

    private final long requestId;
    private final long scheduledAt;
    private final Function<Result<?>, Result<T>> resultParser;
    private CompletableFuture<Result<T>> futureResult;
    private int timeoutSeconds;

    /**
     * This constructor creates an already completed request.
     * This will set the requestId to minus one.
     * The CompletableFuture is immediately completed.
     * @param result the result of the request.
     */
    public Request(Result<T> result) {
        this(-1, System.currentTimeMillis(), CompletableFuture.completedFuture(result));
    }

    public Request(long requestId, CompletableFuture<Result<T>> futureResult) {
        this(requestId, requestId, futureResult);
    }

    public Request(long requestId, long scheduledAt, CompletableFuture<Result<T>> futureResult) {
        this(requestId, scheduledAt, futureResult, DEFAULT_TIMEOUT_SECONDS);
    }

    @SuppressWarnings("unchecked")
    public Request(long requestId, CompletableFuture<Result<T>> futureResult, int timeoutSeconds) {
        this(requestId, requestId, futureResult, timeoutSeconds);
    }

    @SuppressWarnings("unchecked")
    public Request(long requestId, long scheduledAt, CompletableFuture<Result<T>> futureResult, int timeoutSeconds) {
        this.requestId = requestId;
        this.scheduledAt = scheduledAt;
        this.resultParser = (result) -> (Result<T>) result;
        this.futureResult = futureResult;
        if(timeoutSeconds <= 0) timeoutSeconds = 1;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Request(long requestId, Function<Result<?>, Result<T>> resultParser, CompletableFuture<Result<T>> futureResult) {
        this(requestId, requestId, resultParser, futureResult);
    }

    public Request(long requestId, long scheduledAt, Function<Result<?>, Result<T>> resultParser, CompletableFuture<Result<T>> futureResult) {
        this(requestId, scheduledAt, resultParser, futureResult, DEFAULT_TIMEOUT_SECONDS);
    }

    public Request(long requestId, Function<Result<?>, Result<T>> resultParser, CompletableFuture<Result<T>> futureResult, int timeoutSeconds) {
        this(requestId, requestId, resultParser, futureResult, timeoutSeconds);
    }

    public Request(long requestId, long scheduledAt, Function<Result<?>, Result<T>> resultParser, CompletableFuture<Result<T>> futureResult, int timeoutSeconds) {
        this.requestId = requestId;
        this.scheduledAt = scheduledAt;
        this.resultParser = resultParser;
        this.futureResult = futureResult;
        if(timeoutSeconds <= 0) timeoutSeconds = 1;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * The request ID tied to this request.
     * @return The request ID.
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * The time provided by {@link System#currentTimeMillis()} when this request was scheduled.
     * @return The time of schedule.
     */
    public long getScheduledAt() {
        return scheduledAt;
    }

    /**
     * Applies the given function on the result, this is stored.
     * @param fn the function to apply to the result.
     * @return this request.
     */
    public Request<T> thenApply(Function<? super Result<T>, ? extends Result<T>> fn) {
        futureResult = futureResult.thenApply(fn);
        return this;
    }

    /**
     * Creates a new request object with the same request data, although transforms the output of the original request to the new type.
     */
    public <U> Request<U> thenApplyConstruct(Function<? super Result<T>, ? extends Result<U>> fn) {
        return new Request<>(requestId, scheduledAt, null, futureResult.thenApply(fn), timeoutSeconds);
    }

    /**
     * Returns the result parser, that is to be used to properly parse the result before sending the future.
     * @return The result parser.
     */
    protected Function<Result<?>, Result<T>> getResultParser() {
        return resultParser;
    }

    /**
     * The future containing the result.
     * @return The future.
     */
    public CompletableFuture<Result<T>> getFutureResult() {
        return futureResult;
    }

    /**
     * The future containing the result.
     * @return The future.
     */
    public CompletableFuture<Result<T>> getResult() {
        return getFutureResult();
    }

    /**
     * Parses the result type using the parsing function then completes the future.
     * When the result parsing has caused an exception while casting, the future is completed exceptionally.
     * @param result The input generic result.
     * @return {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    public boolean parsedComplete(Result<?> result) {
        try {
            Result<T> parsedResult = resultParser.apply(result);
            return futureResult.complete(parsedResult);
        } catch (Exception e) {
            return futureResult.completeExceptionally(e);
        }
    }

    /**
     * The generic future containing the result.
     * @return A future that casts the result into the parser then into the future.
     */
    public CompletableFuture<Result<?>> getGenericFutureResult() {
        CompletableFuture<Result<?>> generic = new CompletableFuture<>();
        generic.thenApply(resultParser).thenAccept(futureResult::complete).exceptionally(ex -> {
            futureResult.completeExceptionally(ex);
            return null;
        });
        return generic;
    }

    /**
     * The generic future containing the result.
     * @return A future that casts the result into the parser then into the future.
     */
    public CompletableFuture<Result<?>> getGenericResult() {
        return getGenericFutureResult();
    }

    /**
     * Gets the number of seconds this request will time out after it has been scheduled.
     * @return The timeout seconds.
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets number of seconds this request will time out after it has been scheduled.
     * Beware that by setting this to a value that would timeout this request, the canceled state in the {@link CompletableFuture} is not immediately set.
     * @param timeoutSeconds The timeout seconds.
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if(timeoutSeconds <= 0) return;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Returns whether this request is timed out determined by the number of seconds it is defined to timeout for.
     * @return True when the request is timed out, false otherwise.
     */
    public boolean isTimedOut() {
        long timeOutTime = scheduledAt + timeoutSeconds * 1000L;
        return System.currentTimeMillis() >= timeOutTime;
    }

}
