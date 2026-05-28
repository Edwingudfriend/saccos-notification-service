package com.reimagineafrica.notification.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;

@Component
public class WhatsAppClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WhatsAppClient.class);
    private final WebClient.Builder webClientBuilder;

    @Value("${twilio.account-sid}")
    private String accountSid;
    @Value("${twilio.auth-token}")
    private String authToken;
    @Value("${twilio.whatsapp-from}")
    private String from;
    @Value("${twilio.enabled:false}")
    private boolean enabled;

    public WhatsAppClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public void sendWhatsApp(String toPhone, String message) {
        if (!enabled) {
            log.info("WhatsApp disabled — skipping message to {}", toPhone);
            return;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes());

        String to = toPhone.startsWith("whatsapp:") ? toPhone : "whatsapp:" + toPhone;

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", from);
        body.add("To", to);
        body.add("Body", message);

        webClientBuilder.baseUrl("https://api.twilio.com").build()
                .post()
                .uri("/2010-04-01/Accounts/" + accountSid + "/Messages.json")
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    resp -> log.info("WhatsApp sent to {}", to),
                    err  -> { throw new RuntimeException("WhatsApp failed: " + err.getMessage()); }
                );
    }
}
