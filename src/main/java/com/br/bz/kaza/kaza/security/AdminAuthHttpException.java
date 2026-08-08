package com.br.bz.kaza.kaza.security;

import org.springframework.http.HttpStatus;

public class AdminAuthHttpException extends RuntimeException {
    private final HttpStatus status;

    public AdminAuthHttpException(HttpStatus status) {
        this(status, null);
    }

    public AdminAuthHttpException(HttpStatus status, Throwable cause) {
        super(status.getReasonPhrase(), cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
