package com.br.bz.kaza.kaza.service.email;

import com.br.bz.kaza.kaza.domain.LeadEmailDelivery;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryStatus;
import com.br.bz.kaza.kaza.domain.LeadEmailDeliveryType;
import com.br.bz.kaza.kaza.domain.OnboardingLead;
import com.br.bz.kaza.kaza.repository.LeadEmailDeliveryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadEmailOutboxService {
    private final LeadEmailDeliveryRepository deliveries;
    private final EmailProvider provider;
    private final KazaMailProperties properties;

    public LeadEmailOutboxService(LeadEmailDeliveryRepository deliveries, EmailProvider provider,
            KazaMailProperties properties) {
        this.deliveries = deliveries;
        this.provider = provider;
        this.properties = properties;
    }

    @Transactional
    public void queueNotifications(OnboardingLead lead) {
        deliveries.saveAll(List.of(
                new LeadEmailDelivery(
                        lead.getId(),
                        LeadEmailDeliveryType.INTERNAL_NOTIFICATION,
                        properties.from(),
                        properties.leads().internalRecipient(),
                        render(properties.leads().internalSubject(), lead),
                        render(properties.leads().internalBody(), lead)),
                new LeadEmailDelivery(
                        lead.getId(),
                        LeadEmailDeliveryType.PROSPECT_CONFIRMATION,
                        properties.from(),
                        lead.getEmail(),
                        render(properties.leads().prospectSubject(), lead),
                        render(properties.leads().prospectBody(), lead))));
    }

    @Transactional
    public void queueInvitation(OnboardingLead lead, String invitationUrl, java.time.OffsetDateTime expiresAt) {
        String subject = render(properties.leads().invitationSubject(), lead)
                .replace("{{invitationUrl}}", invitationUrl)
                .replace("{{expiresAt}}", text(expiresAt));
        String body = render(properties.leads().invitationBody(), lead)
                .replace("{{invitationUrl}}", invitationUrl)
                .replace("{{expiresAt}}", text(expiresAt));
        deliveries.save(new LeadEmailDelivery(
                lead.getId(),
                LeadEmailDeliveryType.INVITATION,
                properties.from(),
                lead.getEmail(),
                subject,
                body));
    }

    public void dispatchPendingForLead(UUID leadId) {
        dispatchPendingForLead(leadId, null);
    }

    public void dispatchPendingForLead(UUID leadId, Set<LeadEmailDeliveryType> deliveryTypes) {
        List<LeadEmailDelivery> pending = deliveries.findByLeadIdAndStatusOrderByCreatedAtAsc(
                leadId, LeadEmailDeliveryStatus.PENDING);
        List<String> failures = new ArrayList<>();
        for (LeadEmailDelivery delivery : pending) {
            if (deliveryTypes != null && !deliveryTypes.contains(delivery.getType())) {
                continue;
            }
            try {
                delivery.recordAttempt();
                deliveries.save(delivery);
                provider.send(new EmailProvider.EmailMessage(
                        delivery.getFromAddress(),
                        delivery.getRecipient(),
                        delivery.getSubject(),
                        delivery.getBody()));
                delivery.markSent();
                deliveries.save(delivery);
            } catch (RuntimeException exception) {
                delivery.markFailed(exception.getMessage());
                deliveries.save(delivery);
                failures.add(delivery.getType() + ": " + exception.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            throw new EmailDispatchException(String.join("; ", failures));
        }
    }

    public List<LeadEmailDelivery> listForLead(UUID leadId) {
        return deliveries.findByLeadIdOrderByCreatedAtAsc(leadId);
    }

    private String render(String template, OnboardingLead lead) {
        return template
                .replace("{{id}}", text(lead.getId()))
                .replace("{{name}}", text(lead.getName()))
                .replace("{{email}}", text(lead.getEmail()))
                .replace("{{phone}}", text(lead.getPhone()))
                .replace("{{role}}", text(lead.getDeclaredRole()))
                .replace("{{source}}", text(lead.getSource()))
                .replace("{{landingPath}}", text(lead.getLandingPath()))
                .replace("{{createdAt}}", text(lead.getCreatedAt()));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
