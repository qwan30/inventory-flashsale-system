package com.codex.flashsale.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseDomainException {

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}

