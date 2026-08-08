package com.br.bz.kaza.kaza.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.bz.kaza.kaza.repository.OnboardingDocumentRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

class OnboardingDocumentServiceTests {
    private OnboardingApplicationService applications;
    private OnboardingDocumentRepository documents;
    private OnboardingDocumentStorage storage;
    private OnboardingDocumentService service;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applications = mock(OnboardingApplicationService.class);
        documents = mock(OnboardingDocumentRepository.class);
        storage = mock(OnboardingDocumentStorage.class);
        service = new OnboardingDocumentService(applications, documents, storage);
        applicationId = UUID.randomUUID();
    }

    @Test
    void anotherApplicantCannotListDocuments() {
        when(applications.requireEditableOwned(any(), any()))
                .thenThrow(new AccessDeniedException("Application belongs to another applicant"));

        assertThatThrownBy(() -> service.list(applicationId, jwt("other-subject")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anotherApplicantCannotUploadDocuments() {
        when(applications.requireEditableOwned(any(), any()))
                .thenThrow(new AccessDeniedException("Application belongs to another applicant"));

        MultipartFile file = mock(MultipartFile.class);
        assertThatThrownBy(() -> service.upload(applicationId, file, jwt("other-subject")))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Jwt jwt(String subject) {
        return new Jwt(
                "token",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", subject, "email", subject + "@kaza.test"));
    }
}
