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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Handler of requests: a request is defined by a packet that is expected for which a response is needed.
 */
public class RequestHandler {

    private ScheduledExecutorService scheduledExecutor;
    private int computeTimeoutSeconds;
    private ScheduledFuture<?> scheduledComputeTimeout;
    private final HashMap<Long, Request<?>> requests = new HashMap<>(); // TODO Definitely check for concurrency!

    public RequestHandler() {
        this(1);
    }

    public RequestHandler(int computeTimeoutSeconds) {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        if(computeTimeoutSeconds <= 0) computeTimeoutSeconds = 1;
        this.computeTimeoutSeconds = computeTimeoutSeconds;
        scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Gets the number of seconds of how often the requests are checked for their timeouts.
     * @return The number of seconds.
     */
    public int getComputeTimeoutSeconds() {
        return computeTimeoutSeconds;
    }

    /**
     * Sets the number of seconds of how often the requests are checked for their timeouts.
     * After it has been reset, reschedules the task.
     * @param computeTimeoutSeconds The number of seconds.
     */
    public void setComputeTimeoutSeconds(int computeTimeoutSeconds) {
        if(computeTimeoutSeconds <= 0) return;
        this.computeTimeoutSeconds = computeTimeoutSeconds;
        if(isStopped()) return;
        scheduledComputeTimeout.cancel(true);
        scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Cancels all requests and removes the timeout task.
     */
    protected void stop() {
        if(isStopped()) return;
        scheduledComputeTimeout.cancel(true);
        scheduledExecutor.shutdown();
        try {
            if(!scheduledExecutor.awaitTermination(computeTimeoutSeconds * 2L, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
        } finally {
            scheduledComputeTimeout = null;
            requests.values().forEach(request -> request.getFutureResult().cancel(true));
            requests.clear();
        }
    }

    // TODO Move from Controller and Instance so that Connection handles.
    // TODO Upon retrieval of ConnectionClosePacket or KeepAlive has passed, also shutdown this.
    // TODO Actually properly schedule KeepAlive and set their parameters.

    /**
     * Returns whether the executor of the timeout check task is stopped.
     * @return True if it is stopped.
     */
    protected boolean isStopped() {
        return scheduledExecutor.isShutdown();
    }

    /**
     * Starts the timeout task.
     */
    protected void start() {
        if(!isStopped()) return;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Checks for requests that might have been timed out.
     * When a request is timed out, cancel its future and remove it from the requests.
     */
    private void removeTimedOutRequests() {
        ArrayList<Long> timedOutRequests = new ArrayList<>();
        for(Request<?> request : requests.values()) {
            if(request.isTimedOut()) {
                request.getFutureResult().cancel(true);
                timedOutRequests.add(request.getRequestId());
            }
        }
        timedOutRequests.forEach(requests::remove);
    }

    /**
     * Sends the retrieved result into the requests' future.
     * @param requestId The ID of the request.
     * @param result The retrieved result.
     * @return True whether a request with this ID exists.
     * Or {@code true} if this invocation caused the CompletableFuture
     * to transition to a completed state, else {@code false}.
     */
    protected boolean response(long requestId, Result<?> result) {
        if(!requests.containsKey(requestId)) return false;
        Request<?> request = requests.remove(requestId);
        if(request == null) return false;
        return request.parsedComplete(result);
    }

    /**
     * Gets a new request ID that has not yet been requested.
     * @return The new request ID.
     */
    private long getNewRequestId() {
        long id;
        do {
            id = System.currentTimeMillis();
        } while(requests.containsKey(id));
        return id;
    }

    /**
     * Schedules a new request in this request handler.
     * Its response is to be waited for, by default 1 second.
     * This method does not send the request itself over the connection.
     * Any generic results are cast by default.
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    protected <T> Request<T> request() {
        CompletableFuture<Result<T>> future = new CompletableFuture<>();
        Request<T> request = new Request<>(getNewRequestId(), future);
        requests.put(request.getRequestId(), request);
        return request;
    }

    /**
     * Schedules a new request in this request handler.
     * Its response is to be waited for, by default 1 second.
     * This method does not send the request itself over the connection.
     * Any generic results are cast by default.
     * @param timeoutSeconds the number of seconds to wait for the response.
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    protected <T> Request<T> request(int timeoutSeconds) {
        CompletableFuture<Result<T>> future = new CompletableFuture<>();
        Request<T> request = new Request<>(getNewRequestId(), future, timeoutSeconds);
        requests.put(request.getRequestId(), request);
        return request;
    }

    /**
     * Schedules a new request in this request handler.
     * Its response is to be waited for, by default 1 second.
     * This method does not send the request itself over the connection.
     * Any generic results are cast by default.
     * @param resultParser the generic to specific result parser.
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    protected <T> Request<T> request(Function<Result<?>, Result<T>> resultParser) {
        CompletableFuture<Result<T>> future = new CompletableFuture<>();
        Request<T> request = new Request<>(getNewRequestId(), resultParser, future);
        requests.put(request.getRequestId(), request);
        return request;
    }

    /**
     * Schedules a new request in this request handler.
     * Its response is to be waited for, by default 1 second.
     * This method does not send the request itself over the connection.
     * Any generic results are cast by default.
     * @param resultParser the generic to specific result parser.
     * @param timeoutSeconds the number of seconds to wait for the response.
     * @return the request with the future and request ID bundled within it.
     * @param <T> the type of the expected result.
     */
    protected <T> Request<T> request(Function<Result<?>, Result<T>> resultParser, int timeoutSeconds) {
        CompletableFuture<Result<T>> future = new CompletableFuture<>();
        Request<T> request = new Request<>(getNewRequestId(), resultParser, future, timeoutSeconds);
        requests.put(request.getRequestId(), request);
        return request;
    }

}
