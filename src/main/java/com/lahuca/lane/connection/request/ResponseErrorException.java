package com.lahuca.lane.connection.request;

import java.util.StringJoiner;

public class ResponseErrorException extends RuntimeException {

    private final ResponseError responseError;

    public ResponseErrorException(ResponseError responseError) {
        super("An error occurred on the other side of the connection: " + responseError.message());
        this.responseError = responseError;
    }

    public ResponseErrorException(Throwable throwable) {
        this(new ResponseError(throwable));
    }

    public ResponseErrorException(String message) {
        this(new ResponseError(message));
    }

    public ResponseError getResponseError() {
        return responseError;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ResponseErrorException.class.getSimpleName() + "[", "]")
                .add("message='" + getMessage() + "'")
                .add("responseError=" + responseError)
                .toString();
    }
}
