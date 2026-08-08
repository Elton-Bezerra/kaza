package com.br.bz.kaza.kaza.web;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
