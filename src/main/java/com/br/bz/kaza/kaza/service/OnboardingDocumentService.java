package com.br.bz.kaza.kaza.service;

import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingDocument;
import com.br.bz.kaza.kaza.domain.OnboardingDocument.RetentionState;
import com.br.bz.kaza.kaza.repository.OnboardingDocumentRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OnboardingDocumentService {
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_DOCUMENTS = 10;
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "application/pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII),
            "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff},
            "image/png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    private final OnboardingApplicationService applications;
    private final OnboardingDocumentRepository documents;
    private final OnboardingDocumentStorage storage;

    public OnboardingDocumentService(OnboardingApplicationService applications,
            OnboardingDocumentRepository documents, OnboardingDocumentStorage storage) {
        this.applications = applications;
        this.documents = documents;
        this.storage = storage;
    }

    @Transactional
    public OnboardingDocument upload(UUID applicationId, MultipartFile file,
            org.springframework.security.oauth2.jwt.Jwt jwt) {
        OnboardingApplication application = applications.requireEditableOwned(applicationId, jwt);
        if (documents.countByApplicationIdAndRetentionState(applicationId, RetentionState.ACTIVE)
                >= MAX_DOCUMENTS) {
            throw new IllegalArgumentException("An application may have at most 10 active documents");
        }
        byte[] content = validateAndRead(file);
        UUID storageKey = storage.store(content);
        try {
            return documents.save(new OnboardingDocument(
                    application,
                    storageKey,
                    safeFilename(file.getOriginalFilename()),
                    file.getContentType(),
                    content.length,
                    sha256(content)));
        } catch (RuntimeException exception) {
            storage.delete(storageKey);
            throw exception;
        }
    }

    @Transactional
    public List<OnboardingDocument> list(UUID applicationId,
            org.springframework.security.oauth2.jwt.Jwt jwt) {
        applications.requireEditableOwned(applicationId, jwt);
        return documents.findByApplicationIdAndRetentionStateOrderByUploadedAt(
                applicationId, RetentionState.ACTIVE);
    }

    @Transactional
    public void delete(UUID applicationId, UUID documentId,
            org.springframework.security.oauth2.jwt.Jwt jwt) {
        applications.requireEditableOwned(applicationId, jwt);
        OnboardingDocument document = documents.findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding document not found"));
        if (document.getRetentionState() == RetentionState.DELETED) {
            return;
        }
        document.markDeleted();
        storage.delete(document.getStorageKey());
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Document must not exceed 10 MB");
        }
        byte[] signature = SIGNATURES.get(file.getContentType());
        if (signature == null) {
            throw new IllegalArgumentException("Document type must be PDF, JPEG, or PNG");
        }
        try {
            byte[] content = file.getBytes();
            if (!startsWith(content, signature)) {
                throw new IllegalArgumentException("Document content does not match its declared type");
            }
            return content;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded document");
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeFilename(String filename) {
        String value = filename == null ? "document" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isBlank()) {
            value = "document";
        }
        return value.length() > 255 ? value.substring(value.length() - 255) : value;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available");
        }
    }
}
