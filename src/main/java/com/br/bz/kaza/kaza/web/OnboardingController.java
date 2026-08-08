package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingDocument;
import com.br.bz.kaza.kaza.service.CurrentUserService;
import com.br.bz.kaza.kaza.service.OnboardingApplicationService;
import com.br.bz.kaza.kaza.service.OnboardingDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class OnboardingController {
    private final CurrentUserService currentUser;
    private final OnboardingApplicationService applications;
    private final OnboardingDocumentService documents;

    public OnboardingController(CurrentUserService currentUser,
            OnboardingApplicationService applications, OnboardingDocumentService documents) {
        this.currentUser = currentUser;
        this.applications = applications;
        this.documents = documents;
    }

    @GetMapping("/me")
    public OnboardingDtos.MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return currentUser.get(jwt);
    }

    @PostMapping("/onboarding/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingDtos.ApplicationResponse create(
            @RequestBody(required = false) OnboardingDtos.CreateApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return response(applications.create(request, jwt));
    }

    @GetMapping("/onboarding/applications/{id}")
    public OnboardingDtos.ApplicationResponse get(@PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return response(applications.getEditable(id, jwt));
    }

    @PatchMapping("/onboarding/applications/{id}")
    public OnboardingDtos.ApplicationResponse update(@PathVariable UUID id,
            @Valid @RequestBody OnboardingDtos.UpdateApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return response(applications.update(id, request, jwt));
    }

    @PostMapping("/onboarding/applications/{id}/submit")
    public OnboardingDtos.ApplicationResponse submit(@PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return response(applications.submit(id, jwt));
    }

    @PostMapping("/onboarding/applications/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingDtos.DocumentResponse uploadDocument(@PathVariable UUID id,
            @RequestPart("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        return documentResponse(documents.upload(id, file, jwt));
    }

    @GetMapping("/onboarding/applications/{id}/documents")
    public List<OnboardingDtos.DocumentResponse> listDocuments(@PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return documents.list(id, jwt).stream().map(this::documentResponse).toList();
    }

    @DeleteMapping("/onboarding/applications/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt) {
        documents.delete(id, documentId, jwt);
    }

    private OnboardingDtos.ApplicationResponse response(OnboardingApplication application) {
        return new OnboardingDtos.ApplicationResponse(
                application.getId(),
                application.getStatus().name(),
                application.getResponsibleName(),
                application.getResponsibleEmail(),
                application.getResponsiblePhone(),
                application.getTaxId(),
                application.getCondominiumName(),
                application.getAddressLine(),
                application.getAddressCity(),
                application.getAddressState(),
                application.getPostalCode(),
                application.getProposedUnitCount(),
                application.getSubscriptionPricePerUnit(),
                application.getUnits().stream()
                        .map(unit -> new OnboardingDtos.UnitDraftResponse(
                                unit.getIdentifier(), unit.getIdealFraction()))
                        .toList(),
                application.getReviewReason(),
                application.getSubmittedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getVersion());
    }

    private OnboardingDtos.DocumentResponse documentResponse(OnboardingDocument document) {
        return new OnboardingDtos.DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getSha256(),
                document.getScanStatus().name(),
                document.getUploadedAt());
    }
}
