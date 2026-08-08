package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.LandingEvent;
import com.br.bz.kaza.kaza.repository.LandingEventRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/landing-events")
public class LandingEventController {
    private final LandingEventRepository events;

    public LandingEventController(LandingEventRepository events) {
        this.events = events;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OnboardingDtos.LandingEventResponse create(@Valid @RequestBody OnboardingDtos.LandingEventRequest request) {
        LandingEvent event = events.save(new LandingEvent(request));
        return new OnboardingDtos.LandingEventResponse(event.getId(), "ACCEPTED");
    }
}
