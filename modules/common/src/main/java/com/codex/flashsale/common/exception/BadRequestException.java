package com.codex.flashsale.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseDomainException {

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}

