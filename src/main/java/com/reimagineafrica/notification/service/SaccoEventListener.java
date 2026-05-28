package com.reimagineafrica.notification.service;

import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SaccoEventListener {

    private static final Logger log = LoggerFactory.getLogger(SaccoEventListener.class);

    private final NotificationService notificationService;

    // ── member.created ────────────────────────────────────────────

    @RabbitListener(queues = "member.created")
    public void onMemberCreated(Map<String, Object> event) {
        try {
            String memberId     = str(event, "memberId");
            String phoneNumber  = str(event, "phoneNumber");
            String memberNumber = str(event, "memberNumber");
            String tenantId     = str(event, "tenantId");

            notificationService.sendFromTemplate(
                    memberId, memberNumber,
                    NotificationEvent.MEMBER_CREATED,
                    NotificationChannel.SMS,
                    phoneNumber,
                    Map.of("memberNumber", memberNumber),
                    tenantId
            );
            log.info("Processed member.created for memberId={}", memberId);
        } catch (Exception e) {
            log.error("Failed to process member.created event: {}", e.getMessage());
        }
    }

    // ── loan.approved ─────────────────────────────────────────────

    @RabbitListener(queues = "loan.approved")
    public void onLoanApproved(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "memberId"), str(event, "loanId"),
                    NotificationEvent.LOAN_APPROVED, NotificationChannel.SMS,
                    str(event, "phoneNumber"),
                    Map.of(
                        "amount",    str(event, "amount"),
                        "reference", str(event, "loanId")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process loan.approved: {}", e.getMessage());
        }
    }

    // ── loan.rejected ─────────────────────────────────────────────

    @RabbitListener(queues = "loan.rejected")
    public void onLoanRejected(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "memberId"), str(event, "loanId"),
                    NotificationEvent.LOAN_REJECTED, NotificationChannel.SMS,
                    str(event, "phoneNumber"),
                    Map.of(
                        "reference", str(event, "loanId"),
                        "reason",    str(event, "reason")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process loan.rejected: {}", e.getMessage());
        }
    }

    // ── loan.disbursed ────────────────────────────────────────────

    @RabbitListener(queues = "loan.disbursed")
    public void onLoanDisbursed(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "memberId"), str(event, "loanId"),
                    NotificationEvent.LOAN_DISBURSED, NotificationChannel.SMS,
                    str(event, "phoneNumber"),
                    Map.of(
                        "amount",           str(event, "amount"),
                        "reference",        str(event, "loanId"),
                        "firstPaymentDate", str(event, "firstPaymentDate")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process loan.disbursed: {}", e.getMessage());
        }
    }

    // ── contribution.missed ───────────────────────────────────────

    @RabbitListener(queues = "contribution.missed")
    public void onContributionMissed(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "memberId"), str(event, "memberId"),
                    NotificationEvent.CONTRIBUTION_MISSED, NotificationChannel.SMS,
                    str(event, "phoneNumber"),
                    Map.of(
                        "amount",  str(event, "amount"),
                        "dueDate", str(event, "dueDate")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process contribution.missed: {}", e.getMessage());
        }
    }

    // ── guarantor.consent.requested ───────────────────────────────

    @RabbitListener(queues = "guarantor.consent.requested")
    public void onGuarantorConsentRequested(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "guarantorMemberId"), str(event, "loanId"),
                    NotificationEvent.GUARANTOR_CONSENT_REQUESTED, NotificationChannel.SMS,
                    str(event, "guarantorPhone"),
                    Map.of(
                        "borrowerName", str(event, "borrowerName"),
                        "amount",       str(event, "amount"),
                        "reference",    str(event, "loanId")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process guarantor.consent.requested: {}", e.getMessage());
        }
    }

    // ── dividend.declared ─────────────────────────────────────────

    @RabbitListener(queues = "dividend.declared")
    public void onDividendDeclared(Map<String, Object> event) {
        try {
            notificationService.sendFromTemplate(
                    str(event, "memberId"), str(event, "dividendId"),
                    NotificationEvent.DIVIDEND_DECLARED, NotificationChannel.SMS,
                    str(event, "phoneNumber"),
                    Map.of(
                        "year",        str(event, "year"),
                        "amount",      str(event, "amount"),
                        "paymentDate", str(event, "paymentDate")
                    ),
                    str(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process dividend.declared: {}", e.getMessage());
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
