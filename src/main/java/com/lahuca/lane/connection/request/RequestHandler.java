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

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Handler of requests: a request is defined by a packet that is expected for which a response is needed.
 * This contains a {@link ScheduledExecutorService} that can be used for any threads that work on the same connection.
 */
public class RequestHandler {

    private final ScheduledExecutorService scheduledExecutor;
    private int computeTimeoutSeconds;
    private ScheduledFuture<?> scheduledComputeTimeout; // TODO Maybe AtomicReference?
    private final ConcurrentHashMap<Long, Request<?>> requests = new ConcurrentHashMap<>();

    public RequestHandler() {
        this(1);
    }

    public RequestHandler(int computeTimeoutSeconds) {
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        if(computeTimeoutSeconds <= 0) computeTimeoutSeconds = 1;
        this.computeTimeoutSeconds = computeTimeoutSeconds;
        scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Returns the scheduled executor that can be used for any threads that work on the same connection.
     * @return the executor
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
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
        if(scheduledComputeTimeout != null) {
            scheduledComputeTimeout.cancel(true);
            scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * Cancels all requests and removes the timeout task.
     * This does not invalidise the request handler as the schedules executor service is not stopped.
     */
    protected void stopTask() {
        if(scheduledComputeTimeout == null) return;
        scheduledComputeTimeout.cancel(true);
        scheduledComputeTimeout = null;
        requests.forEach((id, request) -> request.getFutureResult().cancel(true));
        requests.clear();
    }

    /**
     * Starts the timeout task.
     */
    protected void startTask() {
        if(scheduledComputeTimeout != null) return;
        scheduledComputeTimeout = scheduledExecutor.scheduleAtFixedRate(this::removeTimedOutRequests, computeTimeoutSeconds, computeTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Cancels all requests and removes the timeout task.
     * This also stops the scheduled executor service;
     * therefore, any other tasks should be canceled before running this method.
     * The service waits for a maximum of double the value of the timeout seconds.
     */
    protected void stopExecutor() {
        if(isStopped()) return;
        if(scheduledComputeTimeout != null) scheduledComputeTimeout.cancel(true);
        scheduledExecutor.shutdown();
        try {
            if(!scheduledExecutor.awaitTermination(computeTimeoutSeconds * 2L, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
        } finally {
            if(scheduledComputeTimeout != null) scheduledComputeTimeout = null;
            requests.forEach((id, request) -> request.getFutureResult().cancel(true));
            requests.clear();
        }
    }

    /**
     * Returns whether the executor of the timeout check task is stopped.
     * @return True if it is stopped.
     */
    protected boolean isStopped() {
        return scheduledExecutor.isShutdown();
    }

    /**
     * Checks for requests that might have been timed out.
     * When a request is timed out, cancel its future and remove it from the requests.
     */
    private void removeTimedOutRequests() {
        requests.forEach((id, request) -> {
            if(request.isTimedOut()) {
                request.getFutureResult().cancel(true);
                requests.remove(id, request);
            }
        });
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
        try {
            System.out.println("Response: " + requestId + " " + requests.containsKey(requestId) + " " + result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Request<?> request = requests.remove(requestId);
        if(request == null) return false;
        return request.parsedComplete(result);
    }

    /**
     * Generates a new unique request ID by using the current timestamp and ensures
     * it is not already present in the requests map.
     * It sets the value to null in the map to indicate that the request is scheduled, to reserve the id (concurrency).
     *
     * @return A unique request ID as a long value.
     */
    private long getNewRequestId() {
        long id;
        do {
            id = System.currentTimeMillis();
        } while(requests.putIfAbsent(id, null) != null);
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
        // TODO Disable requests when it is disabled!!!
        CompletableFuture<Result<T>> future = new CompletableFuture<>();
        Request<T> request = new Request<>(getNewRequestId(), future);
        requests.replace(request.getRequestId(), request);
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
        requests.replace(request.getRequestId(), request);
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
        requests.replace(request.getRequestId(), request);
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
        requests.replace(request.getRequestId(), request);
        return request;
    }

}
