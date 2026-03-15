package com.codex.flashsale.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseDomainException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
