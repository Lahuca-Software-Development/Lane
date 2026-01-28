package com.lahuca.lane.connection.request;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A record that holds the data for when a response is failed.
 * This is parsed from the throwable.
 * @param exceptionType the type of the exception
 * @param message the message of the exception, or a custom type
 * @param stackTrace the stack trace of the exception
 * @param cause the cause of the exception
 */
public record ResponseError(String exceptionType, String message, List<String> stackTrace, ResponseError cause) {

    public static final ResponseError CONTROLLER_DISCONNECTED = new ResponseError("controllerDisconnected");
    public static final ResponseError NO_FREE_SLOTS = new ResponseError("noFreeSlots");
    public static final ResponseError INVALID_ID = new ResponseError("invalidId");
    public static final ResponseError INVALID_PLAYER = new ResponseError("invalidPlayer");
    public static final ResponseError ILLEGAL_STATE = new ResponseError("illegalState");
    public static final ResponseError ILLEGAL_ARGUMENT = new ResponseError("illegalArgument"); // TODO Do this in favor of others above
    public static final ResponseError INSUFFICIENT_RIGHTS = new ResponseError("insufficientRights");

    /*String OK = "ok";
    String UNKNOWN = "unknown";

    String INTERRUPTED = "interrupted";*/

    public ResponseError(String message) {
        this(null, message, null, null);
    }

    public ResponseError(Throwable throwable) {
        this(throwable == null ? null : throwable.getClass().getName(),
                throwable == null ? null : throwable.getMessage(),
                throwable == null ? null : Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString).toList(),
                throwable == null ? null : (throwable.getCause() != null ? new ResponseError(throwable.getCause()) : null));
    }

    public ResponseErrorException exception() {
        return new ResponseErrorException(this);
    }

    public <T> CompletableFuture<T> failedFuture() {
        return CompletableFuture.failedFuture(exception());
    }

}
