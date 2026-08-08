package com.br.bz.kaza.kaza.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

record AdminSessionPrincipal(String subject, String email, String name, List<String> roles) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
