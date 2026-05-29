package com.reimagineafrica.notification.service;

import com.reimagineafrica.notification.client.FcmClient;
import com.reimagineafrica.notification.client.NextSmsClient;
import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.entity.NotificationTemplate;
import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationEvent;
import com.reimagineafrica.notification.enums.NotificationStatus;
import com.reimagineafrica.notification.repository.NotificationRepository;
import com.reimagineafrica.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepo;
    private final NotificationTemplateRepository templateRepo;
    private final NextSmsClient smsClient;
    private final FcmClient fcmClient;
    private final JdbcTemplate jdbcTemplate;

    // ── Send via template ─────────────────────────────────────────

    @Transactional
    public void sendFromTemplate(String memberId, String referenceId,
                                  NotificationEvent event, NotificationChannel channel,
                                  String recipient, Map<String, String> params,
                                  String tenantId) {
        Optional<NotificationTemplate> tpl = templateRepo
                .findByEventAndChannelAndLanguageAndActiveTrue(event, channel, "sw");

        if (tpl.isEmpty()) {
            log.warn("No template found for event={} channel={}", event, channel);
            return;
        }

        String body = tpl.get().render(params);
        String subject = tpl.get().getSubject();

        sendDirect(memberId, referenceId, event, channel, recipient, subject, body, tenantId);
    }

    // ── Send directly (body already composed) ────────────────────

    @Transactional
    public void sendDirect(String memberId, String referenceId,
                            NotificationEvent event, NotificationChannel channel,
                            String recipient, String subject, String body,
                            String tenantId) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .referenceId(referenceId)
                .event(event)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .tenantId(tenantId)
                .build();

        notification = notificationRepo.save(notification);
        dispatch(notification);
    }

    // ── Dispatch to the right channel ────────────────────────────

    @Transactional
    public void dispatch(Notification notification) {
        boolean success = switch (notification.getChannel()) {
            case SMS      -> smsClient.sendSms(notification.getRecipient(), notification.getBody());
            case PUSH     -> fcmClient.sendPush(notification.getRecipient(),
                                                notification.getSubject(),
                                                notification.getBody());
            case WHATSAPP -> {
                log.info("WhatsApp not yet configured — skipping");
                yield false;
            }
            case EMAIL    -> {
                log.info("Email not yet configured — skipping");
                yield false;
            }
        };

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification sent — id={} event={} channel={} to={}",
                    notification.getId(), notification.getEvent(),
                    notification.getChannel(), notification.getRecipient());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("Dispatch returned false");
            log.warn("Notification failed — id={} event={}", notification.getId(), notification.getEvent());
        }
        notificationRepo.save(notification);
    }

    // ── Retry scheduler — runs every 5 minutes ───────────────────

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailed() {
        List<Notification> failed = notificationRepo
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRIES);

        if (failed.isEmpty()) return;
        log.info("Retrying {} failed notifications", failed.size());

        for (Notification n : failed) {
            n.setRetryCount(n.getRetryCount() + 1);
            n.setStatus(NotificationStatus.RETRYING);
            notificationRepo.save(n);
            dispatch(n);
        }
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getByMember(String memberId, Pageable pageable) {
        return notificationRepo.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getByReference(String referenceId, Pageable pageable) {
        return notificationRepo.findByReferenceIdOrderByCreatedAtDesc(referenceId, pageable);
    }

    public String getMemberPhone(String memberId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT phone_number FROM member_db.members WHERE id = ?",
                String.class, memberId);
        } catch (Exception e) {
            log.warn("Could not find phone for member {}: {}", memberId, e.getMessage());
            return "";
        }
    }
}