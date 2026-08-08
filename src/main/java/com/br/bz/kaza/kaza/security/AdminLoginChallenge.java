package com.br.bz.kaza.kaza.security;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

record AdminLoginChallenge(String state, String nonce, String codeVerifier, Instant createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
