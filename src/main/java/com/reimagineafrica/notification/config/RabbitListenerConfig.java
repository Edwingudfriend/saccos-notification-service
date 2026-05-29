package com.reimagineafrica.notification.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationEvent;
import com.reimagineafrica.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RabbitListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitListenerConfig.class);

    private final ConnectionFactory connectionFactory;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SimpleMessageListenerContainer memberCreatedContainer() {
        return container("member.created", message -> {
            try {
                Map<String, Object> e = parse(message);
                log.info("member.created received: memberNumber={}", e.get("memberNumber"));
                notificationService.sendFromTemplate(
                        str(e, "memberId"), str(e, "memberNumber"),
                        NotificationEvent.MEMBER_CREATED, NotificationChannel.SMS,
                        str(e, "phoneNumber"),
                        Map.of("memberNumber", str(e, "memberNumber")),
                        str(e, "tenantId")
                );
            } catch (Exception ex) {
                log.error("Error processing member.created: {}", ex.getMessage());
            }
        });
    }

    @Bean
    public SimpleMessageListenerContainer loanApprovedContainer() {
        return container("loan.approved", message -> {
            try {
                Map<String, Object> e = parse(message);
                log.info("loan.approved received: loanId={}", e.get("loanId"));
                notificationService.sendFromTemplate(
                        str(e, "memberId"), str(e, "loanId"),
                        NotificationEvent.LOAN_APPROVED, NotificationChannel.SMS,
                        str(e, "phoneNumber"),
                        Map.of("amount", str(e, "amount"), "reference", str(e, "loanId")),
                        str(e, "tenantId")
                );
            } catch (Exception ex) {
                log.error("Error processing loan.approved: {}", ex.getMessage());
            }
        });
    }

    @Bean
    public SimpleMessageListenerContainer loanDisbursedContainer() {
        return container("loan.disbursed", message -> {
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
                log.error("Error processing loan.disbursed: {}", ex.getMessage());
            }
        });
    }

    @Bean
    public SimpleMessageListenerContainer contributionMissedContainer() {
        return container("contribution.missed", message -> {
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
                log.error("Error processing contribution.missed: {}", ex.getMessage());
            }
        });
    }

    @Bean
    public SimpleMessageListenerContainer guarantorConsentContainer() {
        return container("guarantor.consent.requested", message -> {
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
                log.error("Error processing guarantor.consent.requested: {}", ex.getMessage());
            }
        });
    }

    @Bean
    public SimpleMessageListenerContainer dividendDeclaredContainer() {
        return container("dividend.declared", message -> {
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
                log.error("Error processing dividend.declared: {}", ex.getMessage());
            }
        });
    }


    @Bean
    public SimpleMessageListenerContainer contributionDepositedContainer() {
        return container("contribution.deposited", message -> {
            try {
                Map<String, Object> e = parse(message);
                log.info("contribution.deposited received: ref={}", e.get("referenceNumber"));
                String memberId = str(e, "memberId");
                String phoneNumber = notificationService.getMemberPhone(memberId);
                log.info("Sending contribution SMS to: {}", phoneNumber);
                notificationService.sendFromTemplate(
                        memberId, str(e, "referenceNumber"),
                        NotificationEvent.CONTRIBUTION_DEPOSITED, NotificationChannel.SMS,
                        phoneNumber,
                        Map.of("amount", str(e, "amount"),
                               "reference", str(e, "referenceNumber"),
                               "month", str(e, "periodMonth"),
                               "year", str(e, "periodYear")),
                        "default"
                );
            } catch (Exception ex) {
                log.error("Error processing contribution.deposited: {}", ex.getMessage());
            }
        });
    }

    private SimpleMessageListenerContainer container(String queue, MessageListener listener) {
        SimpleMessageListenerContainer c = new SimpleMessageListenerContainer(connectionFactory);
        c.setQueueNames(queue);
        c.setMessageListener(listener);
        c.setAutoStartup(true);
        log.info("Registered listener for queue: {}", queue);
        return c;
    }

    private Map<String, Object> parse(Message message) throws Exception {
        return objectMapper.readValue(message.getBody(), new TypeReference<Map<String, Object>>() {});
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
