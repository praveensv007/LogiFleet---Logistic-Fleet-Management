package com.logistics.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Object from, Object to) {
        super("Status cannot change from " + from + " to " + to);
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
