package com.reimagineafrica.notification.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;

@Component
public class NextSmsClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${nextsms.base-url}")
    private String baseUrl;
    @Value("${nextsms.username}")
    private String username;
    @Value("${nextsms.password}")
    private String password;
    @Value("${nextsms.sender-id}")
    private String senderId;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NextSmsClient.class);

    public NextSmsClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public void sendSms(String phoneNumber, String message) {
        String normalized = normalizePhone(phoneNumber);
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());

        webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/api/sms/v1/text/single")
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("from", senderId, "to", normalized, "text", message))
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(
                    resp -> log.info("SMS sent to {} — response: {}", normalized, resp),
                    err  -> { throw new RuntimeException("NextSMS failed: " + err.getMessage()); }
                );
    }

    private String normalizePhone(String phone) {
        // Ensure Tanzania format: +255XXXXXXXXX
        if (phone.startsWith("0")) return "+255" + phone.substring(1);
        if (phone.startsWith("255")) return "+" + phone;
        return phone;
    }
}
