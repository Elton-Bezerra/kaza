package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.domain.User;
import com.br.bz.kaza.kaza.repository.UserRepository;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }

    @Transactional
    public User ensureFromJwt(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT subject is required");
        }
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        return users.findBySubject(jwt.getSubject())
                .map(existing -> {
                    if (!Objects.equals(existing.getEmail(), email) || !Objects.equals(existing.getDisplayName(), name)) {
                        existing.refresh(email, name);
                    }
                    return existing;
                })
                .orElseGet(() -> users.save(new User(jwt.getSubject(), email, name)));
    }

    @Transactional
    public User ensureFromSubject(String subject, String displayName) {
        if (subject == null || subject.isBlank()) return null;
        return users.findBySubject(subject)
                .orElseGet(() -> users.save(new User(subject, null, displayName)));
    }
}
