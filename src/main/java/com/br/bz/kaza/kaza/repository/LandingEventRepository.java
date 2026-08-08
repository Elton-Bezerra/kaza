package com.br.bz.kaza.kaza.repository;

import com.br.bz.kaza.kaza.domain.LandingEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandingEventRepository extends JpaRepository<LandingEvent, UUID> {
}
