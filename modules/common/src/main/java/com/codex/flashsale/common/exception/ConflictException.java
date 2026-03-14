package com.codex.flashsale.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseDomainException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}

