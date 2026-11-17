package com.logistics.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    /** Optional field the error belongs to, so the React form can show it under the input. */
    private final String field;

    public BadRequestException(String message) {
        super(message);
        this.field = null;
    }

    public BadRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
