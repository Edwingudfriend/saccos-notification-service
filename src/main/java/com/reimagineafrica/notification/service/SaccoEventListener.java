package com.reimagineafrica.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "member.created")
    public void onMemberCreated(String message) {
        try {
            Map<String, Object> e = parse(message);
            log.info("Received member.created event: {}", e.get("memberNumber"));
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "memberNumber"),
                    NotificationEvent.MEMBER_CREATED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("memberNumber", str(e, "memberNumber")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process member.created: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "loan.approved")
    public void onLoanApproved(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "loanId"),
                    NotificationEvent.LOAN_APPROVED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("amount", str(e, "amount"), "reference", str(e, "loanId")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process loan.approved: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "loan.rejected")
    public void onLoanRejected(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "loanId"),
                    NotificationEvent.LOAN_REJECTED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("reference", str(e, "loanId"), "reason", str(e, "reason")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process loan.rejected: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "loan.disbursed")
    public void onLoanDisbursed(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "loanId"),
                    NotificationEvent.LOAN_DISBURSED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("amount", str(e, "amount"), "reference", str(e, "loanId"),
                           "firstPaymentDate", str(e, "firstPaymentDate")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process loan.disbursed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "contribution.missed")
    public void onContributionMissed(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "memberId"),
                    NotificationEvent.CONTRIBUTION_MISSED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("amount", str(e, "amount"), "dueDate", str(e, "dueDate")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process contribution.missed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "guarantor.consent.requested")
    public void onGuarantorConsentRequested(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "guarantorMemberId"), str(e, "loanId"),
                    NotificationEvent.GUARANTOR_CONSENT_REQUESTED, NotificationChannel.SMS,
                    str(e, "guarantorPhone"),
                    Map.of("borrowerName", str(e, "borrowerName"),
                           "amount", str(e, "amount"), "reference", str(e, "loanId")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process guarantor.consent.requested: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "dividend.declared")
    public void onDividendDeclared(String message) {
        try {
            Map<String, Object> e = parse(message);
            notificationService.sendFromTemplate(
                    str(e, "memberId"), str(e, "dividendId"),
                    NotificationEvent.DIVIDEND_DECLARED, NotificationChannel.SMS,
                    str(e, "phoneNumber"),
                    Map.of("year", str(e, "year"), "amount", str(e, "amount"),
                           "paymentDate", str(e, "paymentDate")),
                    str(e, "tenantId")
            );
        } catch (Exception ex) {
            log.error("Failed to process dividend.declared: {}", ex.getMessage());
        }
    }

    private Map<String, Object> parse(String message) throws Exception {
        return objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
