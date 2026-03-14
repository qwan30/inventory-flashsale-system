package com.codex.flashsale.common.exception;

import org.springframework.http.HttpStatus;

public class BusyResourceException extends BaseDomainException {

    public BusyResourceException(String message) {
        super(HttpStatus.LOCKED, "RESOURCE_LOCKED", message);
    }
}

