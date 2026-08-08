package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.OnboardingDtos;
import com.br.bz.kaza.kaza.domain.OnboardingApplication;
import com.br.bz.kaza.kaza.domain.OnboardingDocument;
import com.br.bz.kaza.kaza.service.OnboardingApplicationInvitationService;
import com.br.bz.kaza.kaza.service.OnboardingApplicationService;
import com.br.bz.kaza.kaza.service.OnboardingDocumentService;
import com.br.bz.kaza.kaza.service.PublicOnboardingSession;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/public/onboarding")
public class PublicOnboardingController {
    static final String SESSION_ATTRIBUTE = PublicOnboardingController.class.getName() + ".SESSION";

    private final OnboardingApplicationInvitationService invitations;
    private final OnboardingApplicationService applications;
    private final OnboardingDocumentService documents;

    public PublicOnboardingController(OnboardingApplicationInvitationService invitations,
            OnboardingApplicationService applications,
            OnboardingDocumentService documents) {
        this.invitations = invitations;
        this.applications = applications;
        this.documents = documents;
    }

    @GetMapping("/invitations/{token}")
    public OnboardingDtos.ApplicationInvitationResponse viewInvitation(@PathVariable String token) {
        var invitation = invitations.viewInvitation(token);
        return new OnboardingDtos.ApplicationInvitationResponse(
                invitation.id(),
                invitation.leadId(),
                invitation.leadName(),
                invitation.leadEmail(),
                invitation.status(),
                invitation.expiresAt(),
                invitation.acceptedAt(),
                invitation.applicationId());
    }

    @PostMapping("/invitations/{token}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingDtos.ApplicationResponse acceptInvitation(@PathVariable String token, HttpSession session) {
        var acceptance = invitations.acceptInvitation(token);
        session.setAttribute(SESSION_ATTRIBUTE, acceptance.session());
        session.setMaxInactiveInterval((int) java.time.Duration.ofDays(7).toSeconds());
        return response(acceptance.application());
    }

    @GetMapping("/application")
    public OnboardingDtos.ApplicationResponse currentApplication(HttpSession session) {
        return response(requireCurrentApplication(session));
    }

    @PatchMapping("/application")
    public OnboardingDtos.ApplicationResponse updateApplication(
            @Valid @RequestBody OnboardingDtos.UpdateApplicationRequest request,
            HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        return response(applications.update(current.applicationId(), request, current.toJwt()));
    }

    @PostMapping("/application/submit")
    public OnboardingDtos.ApplicationResponse submitApplication(HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        return response(applications.submit(current.applicationId(), current.toJwt()));
    }

    @PostMapping("/application/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingDtos.DocumentResponse uploadDocument(@RequestPart("file") MultipartFile file, HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        return documentResponse(documents.upload(current.applicationId(), file, current.toJwt()));
    }

    @GetMapping("/application/documents")
    public List<OnboardingDtos.DocumentResponse> listDocuments(HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        return documents.list(current.applicationId(), current.toJwt()).stream()
                .map(this::documentResponse)
                .toList();
    }

    @DeleteMapping("/application/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID documentId, HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        documents.delete(current.applicationId(), documentId, current.toJwt());
    }

    private PublicOnboardingSession requireSession(HttpSession session) {
        PublicOnboardingSession current = (PublicOnboardingSession) session.getAttribute(SESSION_ATTRIBUTE);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invitation session is required");
        }
        return current;
    }

    private OnboardingApplication requireCurrentApplication(HttpSession session) {
        PublicOnboardingSession current = requireSession(session);
        return applications.getOwned(current.applicationId(), current.toJwt());
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
                        .map(unit -> new OnboardingDtos.UnitDraftResponse(unit.getIdentifier(), unit.getIdealFraction()))
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
