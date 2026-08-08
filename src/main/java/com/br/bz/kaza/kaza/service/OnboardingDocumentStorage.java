package com.br.bz.kaza.kaza.service;

import java.util.UUID;

public interface OnboardingDocumentStorage {
    UUID store(byte[] content);
    byte[] load(UUID storageKey);
    void delete(UUID storageKey);
}
