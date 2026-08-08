package com.br.bz.kaza.kaza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "onboarding_application_units")
public class OnboardingApplicationUnit {
    @Id
    @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private OnboardingApplication application;
    @Column(nullable = false, length = 100)
    private String identifier;
    @Column(name = "ideal_fraction", nullable = false, precision = 12, scale = 8)
    private BigDecimal idealFraction;
    @Column(nullable = false)
    private int position;

    protected OnboardingApplicationUnit() {
    }

    public OnboardingApplicationUnit(OnboardingApplication application, String identifier,
            BigDecimal idealFraction, int position) {
        this.application = application;
        this.identifier = identifier;
        this.idealFraction = idealFraction;
        this.position = position;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BigDecimal getIdealFraction() {
        return idealFraction;
    }
}
