package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.domain.OnboardingDocumentObject;
import com.br.bz.kaza.kaza.repository.OnboardingDocumentObjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DatabaseOnboardingDocumentStorage implements OnboardingDocumentStorage {
    private final OnboardingDocumentObjectRepository objects;

    public DatabaseOnboardingDocumentStorage(OnboardingDocumentObjectRepository objects) {
        this.objects = objects;
    }

    @Override
    public UUID store(byte[] content) {
        UUID storageKey = UUID.randomUUID();
        objects.save(new OnboardingDocumentObject(storageKey, content));
        return storageKey;
    }

    @Override
    public byte[] load(UUID storageKey) {
        return objects.findById(storageKey)
                .orElseThrow(() -> new IllegalArgumentException("Document object not found"))
                .getContent();
    }

    @Override
    public void delete(UUID storageKey) {
        objects.deleteById(storageKey);
    }
}
