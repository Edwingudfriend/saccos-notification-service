package com.reimagineafrica.notification.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NextSmsClient {

    private static final Logger log = LoggerFactory.getLogger(NextSmsClient.class);

    private final WebClient.Builder webClientBuilder;

    @Value("${nextsms.username}")
    private String username;

    @Value("${nextsms.password}")
    private String password;

    @Value("${nextsms.sender-id}")
    private String senderId;

    @Value("${nextsms.base-url}")
    private String baseUrl;

    public boolean sendSms(String phoneNumber, String message) {
        if (username == null || username.isBlank()) {
            log.warn("NextSMS not configured — skipping SMS to {}", phoneNumber);
            return false;
        }
        try {
            String phone = normalizePhone(phoneNumber);
            Map<String, Object> body = Map.of(
                "from",    senderId,
                "to",      phone,
                "text",    message
            );
            String response = webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBasicAuth(username, password))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent to {} — response: {}", phone, response);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        phone = phone.replaceAll("[^0-9+]", "");
        if (phone.startsWith("0")) phone = "255" + phone.substring(1);
        if (phone.startsWith("+")) phone = phone.substring(1);
        return phone;
    }
}
