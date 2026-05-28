package com.reimagineafrica.notification.service;

import com.reimagineafrica.notification.client.FcmClient;
import com.reimagineafrica.notification.client.NextSmsClient;
import com.reimagineafrica.notification.client.WhatsAppClient;
import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.entity.NotificationTemplate;
import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationStatus;
import com.reimagineafrica.notification.repository.NotificationRepository;
import com.reimagineafrica.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifRepo;
    private final NotificationTemplateRepository templateRepo;
    private final NextSmsClient smsClient;
    private final FcmClient fcmClient;
    private final WhatsAppClient whatsAppClient;

    @Value("${notification.default-language:SW}")
    private String defaultLanguage;

    // ══════════════════════════════════════════════════════════════
    // SEND — main entry point called by event listeners
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public void send(String recipientId, String phone, String fcmToken,
                     String eventType, NotificationChannel channel,
                     Map<String, String> variables,
                     String referenceType, String referenceId, String tenantId) {

        // Look up template
        Optional<NotificationTemplate> templateOpt = templateRepo.findTemplate(
                eventType, channel.name(), defaultLanguage);

        if (templateOpt.isEmpty()) {
            log.warn("No template found — event={}, channel={}", eventType, channel);
            return;
        }

        NotificationTemplate tmpl = templateOpt.get();
        String message = tmpl.render(variables);

        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .recipientPhone(phone)
                .recipientFcm(fcmToken)
                .channel(channel)
                .eventType(eventType)
                .subject(tmpl.getSubject())
                .message(message)
                .language(tmpl.getLanguage())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .tenantId(tenantId)
                .build();

        notif = notifRepo.save(notif);
        dispatch(notif);
    }

    /** Send a direct message without a template (e.g. OTP already formatted) */
    @Transactional
    public void sendDirect(String recipientId, String phone, String fcmToken,
                           String eventType, NotificationChannel channel,
                           String message, String tenantId) {

        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .recipientPhone(phone)
                .recipientFcm(fcmToken)
                .channel(channel)
                .eventType(eventType)
                .message(message)
                .tenantId(tenantId)
                .build();

        notif = notifRepo.save(notif);
        dispatch(notif);
    }

    // ══════════════════════════════════════════════════════════════
    // DISPATCH — routes to the correct client
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public void dispatch(Notification notif) {
        try {
            switch (notif.getChannel()) {
                case SMS -> {
                    if (notif.getRecipientPhone() == null)
                        throw new RuntimeException("No phone number for SMS");
                    smsClient.sendSms(notif.getRecipientPhone(), notif.getMessage());
                }
                case PUSH -> {
                    if (notif.getRecipientFcm() == null)
                        throw new RuntimeException("No FCM token for PUSH");
                    fcmClient.sendPush(notif.getRecipientFcm(),
                            notif.getSubject() != null ? notif.getSubject() : notif.getEventType(),
                            notif.getMessage());
                }
                case WHATSAPP -> {
                    if (notif.getRecipientPhone() == null)
                        throw new RuntimeException("No phone number for WhatsApp");
                    whatsAppClient.sendWhatsApp(notif.getRecipientPhone(), notif.getMessage());
                }
                default -> log.warn("Unsupported channel: {}", notif.getChannel());
            }

            notif.markSent();
            notifRepo.save(notif);
            log.info("Notification sent — id={}, channel={}, event={}",
                    notif.getId(), notif.getChannel(), notif.getEventType());

        } catch (Exception e) {
            notif.markFailed(e.getMessage());
            notifRepo.save(notif);
            log.error("Notification failed — id={}, error={}", notif.getId(), e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // RETRY — runs every 5 minutes for failed notifications
    // ══════════════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void retryFailed() {
        List<Notification> retryable = notifRepo.findRetryable();
        if (!retryable.isEmpty()) {
            log.info("Retrying {} failed notifications", retryable.size());
            retryable.forEach(this::dispatch);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // QUERY
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<Notification> getForRecipient(String recipientId, Pageable pageable) {
        return notifRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getForReference(String referenceType, String referenceId) {
        return notifRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }
}
