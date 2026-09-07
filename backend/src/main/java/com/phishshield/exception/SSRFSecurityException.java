package com.phishshield.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SSRFSecurityException extends RuntimeException {
    public SSRFSecurityException(String message) {
        super(message);
    }
}
