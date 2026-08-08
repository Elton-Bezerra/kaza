package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findBySubject(String subject);
}
