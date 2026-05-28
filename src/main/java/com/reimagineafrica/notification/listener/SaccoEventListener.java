package com.reimagineafrica.notification.listener;

import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to ALL SACCOS RabbitMQ events and triggers notifications.
 * Each handler extracts relevant data, builds the template variables,
 * and calls NotificationService.send().
 */
@Component
@RequiredArgsConstructor
public class SaccoEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SaccoEventListener.class);

    private final NotificationService notifService;

    // ── Member created — welcome SMS ──────────────────────────────

    @RabbitListener(queues = "member.created")
    public void onMemberCreated(Map<String, Object> event) {
        try {
            log.info("Event received: member.created — memberId={}", event.get("memberId"));

            Map<String, String> vars = new HashMap<>();
            vars.put("name",         getStr(event, "fullName"));
            vars.put("memberNumber", getStr(event, "memberNumber"));

            notifService.send(
                    getStr(event, "userId"),
                    getStr(event, "phoneNumber"),
                    null,  // no FCM token at registration time
                    "MEMBER_CREATED",
                    NotificationChannel.SMS,
                    vars,
                    "MEMBER", getStr(event, "memberId"),
                    getStr(event, "tenantId")
            );
        } catch (Exception e) {
            log.error("Failed to process member.created event: {}", e.getMessage());
        }
    }

    // ── Loan approved ─────────────────────────────────────────────

    @RabbitListener(queues = "loan.approved")
    public void onLoanApproved(Map<String, Object> event) {
        try {
            log.info("Event received: loan.approved — loanId={}", event.get("loanId"));

            Map<String, String> vars = new HashMap<>();
            vars.put("name",   getStr(event, "memberName"));
            vars.put("amount", formatAmount(event.get("amount")));

            // Send both SMS and PUSH
            notifService.send(
                    getStr(event, "userId"), getStr(event, "phoneNumber"), getStr(event, "fcmToken"),
                    "LOAN_APPROVED", NotificationChannel.SMS, vars,
                    "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));

            if (event.get("fcmToken") != null) {
                notifService.send(
                        getStr(event, "userId"), null, getStr(event, "fcmToken"),
                        "LOAN_APPROVED", NotificationChannel.PUSH, vars,
                        "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));
            }
        } catch (Exception e) {
            log.error("Failed to process loan.approved event: {}", e.getMessage());
        }
    }

    // ── Loan rejected ─────────────────────────────────────────────

    @RabbitListener(queues = "loan.rejected")
    public void onLoanRejected(Map<String, Object> event) {
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("name",   getStr(event, "memberName"));
            vars.put("amount", formatAmount(event.get("amount")));
            vars.put("reason", getStr(event, "rejectionReason"));

            notifService.send(
                    getStr(event, "userId"), getStr(event, "phoneNumber"), null,
                    "LOAN_REJECTED", NotificationChannel.SMS, vars,
                    "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));
        } catch (Exception e) {
            log.error("Failed to process loan.rejected event: {}", e.getMessage());
        }
    }

    // ── Loan disbursed ────────────────────────────────────────────

    @RabbitListener(queues = "loan.disbursed")
    public void onLoanDisbursed(Map<String, Object> event) {
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("name",    getStr(event, "memberName"));
            vars.put("amount",  formatAmount(event.get("amount")));
            vars.put("dueDate", getStr(event, "firstDueDate"));

            notifService.send(
                    getStr(event, "userId"), getStr(event, "phoneNumber"), getStr(event, "fcmToken"),
                    "LOAN_DISBURSED", NotificationChannel.SMS, vars,
                    "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));

            if (event.get("fcmToken") != null) {
                notifService.send(
                        getStr(event, "userId"), null, getStr(event, "fcmToken"),
                        "LOAN_DISBURSED", NotificationChannel.PUSH, vars,
                        "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));
            }
        } catch (Exception e) {
            log.error("Failed to process loan.disbursed event: {}", e.getMessage());
        }
    }

    // ── Contribution missed ───────────────────────────────────────

    @RabbitListener(queues = "contribution.missed")
    public void onContributionMissed(Map<String, Object> event) {
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("name",    getStr(event, "memberName"));
            vars.put("amount",  formatAmount(event.get("amount")));
            vars.put("dueDate", getStr(event, "dueDate"));

            notifService.send(
                    getStr(event, "userId"), getStr(event, "phoneNumber"), null,
                    "CONTRIBUTION_MISSED", NotificationChannel.SMS, vars,
                    "CONTRIBUTION", getStr(event, "contributionId"), getStr(event, "tenantId"));
        } catch (Exception e) {
            log.error("Failed to process contribution.missed event: {}", e.getMessage());
        }
    }

    // ── Guarantor consent requested ───────────────────────────────

    @RabbitListener(queues = "guarantor.consent.requested")
    public void onGuarantorConsent(Map<String, Object> event) {
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("name",         getStr(event, "guarantorName"));
            vars.put("borrowerName", getStr(event, "borrowerName"));
            vars.put("amount",       formatAmount(event.get("amount")));

            // Try WhatsApp first, fall back to SMS
            notifService.send(
                    getStr(event, "guarantorUserId"), getStr(event, "guarantorPhone"), null,
                    "GUARANTOR_CONSENT_REQUESTED", NotificationChannel.WHATSAPP, vars,
                    "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));

            notifService.send(
                    getStr(event, "guarantorUserId"), getStr(event, "guarantorPhone"), null,
                    "GUARANTOR_CONSENT_REQUESTED", NotificationChannel.SMS, vars,
                    "LOAN", getStr(event, "loanId"), getStr(event, "tenantId"));
        } catch (Exception e) {
            log.error("Failed to process guarantor.consent.requested event: {}", e.getMessage());
        }
    }

    // ── Dividend declared ─────────────────────────────────────────

    @RabbitListener(queues = "dividend.declared")
    public void onDividendDeclared(Map<String, Object> event) {
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("name",      getStr(event, "memberName"));
            vars.put("amount",    formatAmount(event.get("totalDividend")));
            vars.put("yourShare", formatAmount(event.get("memberShare")));
            vars.put("shares",    getStr(event, "shares"));

            notifService.send(
                    getStr(event, "userId"), getStr(event, "phoneNumber"), getStr(event, "fcmToken"),
                    "DIVIDEND_DECLARED", NotificationChannel.SMS, vars,
                    "DIVIDEND", getStr(event, "dividendId"), getStr(event, "tenantId"));
        } catch (Exception e) {
            log.error("Failed to process dividend.declared event: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String formatAmount(Object amount) {
        if (amount == null) return "0";
        try {
            double d = Double.parseDouble(amount.toString());
            return String.format("%,.0f", d);
        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }
}
