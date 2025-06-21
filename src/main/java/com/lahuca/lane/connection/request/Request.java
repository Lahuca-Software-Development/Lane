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
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A request is defined by a packet that is expected for which a response is needed.
 * A request and its response are combined using request IDs.
 * When a request is sent over a connection, its response is to be expected to be cast to the expected type.
 * This is only the case when the returned result is {@link ResponsePacket#OK}, otherwise a {@link ResultUnsuccessfulException} is thrown with the error.
 * The {@link CompletableFuture} handles the asynchronous response.
 * This {@link CompletableFuture} is canceled when no response is received within a set timeframe: the timeout.
 * As results are frequent of the generic unknown type ?, the result parser is to be used to properly parse the result before sending the future.
 *
 * @param <T> The result type.
 */
public class Request<T> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 3;

    private final long requestId;
    private final long scheduledAt;
    private final Function<Object, T> resultParser;
    private CompletableFuture<T> futureResult;
    private int timeoutSeconds;

    /**
     * This constructor creates an already successfully completed request.
     * This will set the requestId to minus one.
     * The CompletableFuture is immediately completed.
     *
     * @param result the result of the request.
     */
    public Request(T result) {
        this(-1, System.currentTimeMillis(), CompletableFuture.completedFuture(result));
    }

    /**
     * This constructor creates an already unsuccessfully completed request.
     * This will set the requestId to minus one.
     * The CompletableFuture is immediately failed.
     * @param result the result of the request
     */
    public Request(ResultUnsuccessfulException result) {
        this(-1, System.currentTimeMillis(), CompletableFuture.failedFuture(result));
    }

    public Request(long requestId, CompletableFuture<T> futureResult) {
        this(requestId, requestId, futureResult);
    }

    public Request(long requestId, long scheduledAt, CompletableFuture<T> futureResult) {
        this(requestId, scheduledAt, futureResult, DEFAULT_TIMEOUT_SECONDS);
    }

    public Request(long requestId, CompletableFuture<T> futureResult, int timeoutSeconds) {
        this(requestId, requestId, futureResult, timeoutSeconds);
    }

    @SuppressWarnings("unchecked")
    public Request(long requestId, long scheduledAt, CompletableFuture<T> futureResult, int timeoutSeconds) {
        this.requestId = requestId;
        this.scheduledAt = scheduledAt;
        this.resultParser = (result) -> (T) result;
        this.futureResult = futureResult;
        if (timeoutSeconds <= 0) timeoutSeconds = 1;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Request(long requestId, Function<Object, T> resultParser, CompletableFuture<T> futureResult) {
        this(requestId, requestId, resultParser, futureResult);
    }

    public Request(long requestId, long scheduledAt, Function<Object, T> resultParser, CompletableFuture<T> futureResult) {
        this(requestId, scheduledAt, resultParser, futureResult, DEFAULT_TIMEOUT_SECONDS);
    }

    public Request(long requestId, Function<Object, T> resultParser, CompletableFuture<T> futureResult, int timeoutSeconds) {
        this(requestId, requestId, resultParser, futureResult, timeoutSeconds);
    }

    public Request(long requestId, long scheduledAt, Function<Object, T> resultParser, CompletableFuture<T> futureResult, int timeoutSeconds) {
        this.requestId = requestId;
        this.scheduledAt = scheduledAt;
        this.resultParser = resultParser;
        this.futureResult = futureResult;
        if (timeoutSeconds <= 0) timeoutSeconds = 1;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * The request ID tied to this request.
     *
     * @return The request ID.
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * The time provided by {@link System#currentTimeMillis()} when this request was scheduled.
     *
     * @return The time of schedule.
     */
    public long getScheduledAt() {
        return scheduledAt;
    }

    /**
     * Applies the given function on the result, this is stored.
     *
     * @param fn the function to apply to the result.
     * @return this request.
     */
    public Request<T> thenApply(Function<? super T, ? extends T> fn) {
        futureResult = futureResult.thenApply(fn);
        return this;
    }

    /**
     * Creates a new request object with the same request data: request ID, schedule timestamp and timeout seconds.
     * The newly created request object has the expected output after applying the provided function on the original request.
     * Whenever the request has failed, it can be listened using the failed function. Modification can be done via {@link #thenApplyConstruct(Function, Function)}.
     * When manually providing the new object with a result, the original stays intact.
     * The original object always influences the new object.
     *
     * @param failed the function that is run when the original function fails, a new result can be provided
     * @param apply the function to run on the result
     * @return the newly created request object
     * @param <U> the type of the newly expected output
     */
    public <U> Request<U> thenApplyConstruct(Consumer<ResultUnsuccessfulException> failed, Function<? super T, ? extends U> apply) {
        return new Request<>(requestId, scheduledAt, null, futureResult.whenComplete((data, ex) -> {
            if(ex != null) {
                if(ex instanceof ResultUnsuccessfulException resultEx) {
                    failed.accept(resultEx);
                }
            }
        }).thenApply(apply), timeoutSeconds);
    }

    /**
     * Creates a new request object with the same request data: request ID, schedule timestamp and timeout seconds.
     * The newly created request object has the expected output after applying the provided function on the original request.
     * Whenever the request has failed, it can be listened and modified using the failed function.
     * When manually providing the new object with a result, the original stays intact.
     * The original object always influences the new object.
     *
     * @param failed the function that is run when the original function fails, a new result can be provided
     * @param apply the function to run on the result
     * @return the newly created request object
     * @param <U> the type of the newly expected output
     */
    public <U> Request<U> thenApplyConstruct(Function<ResultUnsuccessfulException, ? extends U> failed, Function<? super T, ? extends U> apply) {
        return new Request<>(requestId, scheduledAt, null, futureResult.handle((data, ex) -> {
            if(ex != null) {
                if(ex instanceof ResultUnsuccessfulException resultEx) {
                    return failed.apply(resultEx);
                }
                throw new CompletionException(ex);
            }
            return apply.apply(data);
        }), timeoutSeconds);
    }

    /**
     * Creates a new request object with the same request data: request ID, schedule timestamp and timeout seconds.
     * The newly created request object has the expected output after applying the provided function on the original request.
     * Whenever the request has failed, it is passed onto the newly created request.
     * When manually providing the new object with a result, the original stays intact.
     * The original object always influences the new object.
     *
     * @param apply the function to run on the result
     * @return the newly created request object
     * @param <U> the type of the newly expected output
     */
    public <U> Request<U> thenApplyConstruct(Function<? super T, ? extends U> apply) {
        return new Request<>(requestId, scheduledAt, null, futureResult.thenApply(apply), timeoutSeconds);
    }

    /**
     * Returns the result parser, that is to be used to properly parse the result before sending the future.
     *
     * @return The result parser.
     */
    protected Function<Object, T> getResultParser() {
        return resultParser;
    }

    /**
     * The future containing the result.
     *
     * @return The future.
     */
    public CompletableFuture<T> getFutureResult() {
        return futureResult;
    }

    /**
     * The future containing the result.
     *
     * @return The future.
     */
    public CompletableFuture<T> getResult() {
        return getFutureResult();
    }

    /**
     * Parses the result type using the parsing function then completes the future.
     * When the result parsing has caused an exception while casting, the future is completed exceptionally.
     *
     * @param response The input generic response.
     * @return {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    public <U> boolean parsedComplete(ResponsePacket<U> response) {
        if(response.getResult().equals(ResponsePacket.OK)) { // TODO Maybe somewhere else
            try {
                T parsedResult = resultParser.apply(response.getData());
                return futureResult.complete(parsedResult);
            } catch (Exception e) {
                return futureResult.completeExceptionally(e);
            }
        }
        return futureResult.completeExceptionally(new ResultUnsuccessfulException(response.getResult()));
    }

    /**
     * The generic future containing the result.
     *
     * @return A future that casts the result into the parser then into the future.
     */
    public CompletableFuture<Object> getGenericFutureResult() {
        CompletableFuture<Object> generic = new CompletableFuture<>();
        generic.thenApply(resultParser).thenAccept(futureResult::complete).exceptionally(ex -> {
            futureResult.completeExceptionally(ex);
            return null;
        });
        return generic;
    }

    /**
     * The generic future containing the result.
     *
     * @return A future that casts the result into the parser then into the future.
     */
    public CompletableFuture<Object> getGenericResult() {
        return getGenericFutureResult();
    }

    /**
     * Gets the number of seconds this request will time out after it has been scheduled.
     *
     * @return The timeout seconds.
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets the number of seconds this request will time out after it has been scheduled.
     * Beware that by setting this to a value that would timeout this request, the canceled state in the {@link CompletableFuture} is not immediately set.
     *
     * @param timeoutSeconds The timeout seconds.
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds <= 0) return;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Returns whether this request is timed out determined by the number of seconds it is defined to timeout for.
     *
     * @return True when the request is timed out, false otherwise.
     */
    public boolean isTimedOut() {
        long timeOutTime = getScheduledAt() + getTimeoutSeconds() * 1000L;
        return System.currentTimeMillis() >= timeOutTime;
    }

}
