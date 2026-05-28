package com.reimagineafrica.notification.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

// ─────────────────────────────────────────────────────────────────
// FCM — Firebase Cloud Messaging (Flutter mobile push)
// ─────────────────────────────────────────────────────────────────
@Component
public class FcmClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FcmClient.class);
    private final WebClient.Builder webClientBuilder;

    @Value("${fcm.server-key}")
    private String serverKey;

    @Value("${fcm.enabled:false}")
    private boolean enabled;

    public FcmClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public void sendPush(String fcmToken, String title, String body) {
        if (!enabled) {
            log.info("FCM disabled — skipping push to token {}***", fcmToken.substring(0, Math.min(10, fcmToken.length())));
            return;
        }

        Map<String, Object> payload = Map.of(
            "to", fcmToken,
            "notification", Map.of("title", title, "body", body),
            "data", Map.of("event", title)
        );

        webClientBuilder.baseUrl("https://fcm.googleapis.com").build()
                .post()
                .uri("/fcm/send")
                .header("Authorization", "key=" + serverKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(
                    resp -> log.info("Push sent — success={}", resp),
                    err  -> { throw new RuntimeException("FCM failed: " + err.getMessage()); }
                );
    }
}
