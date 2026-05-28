package com.reimagineafrica.notification.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Firebase Cloud Messaging push notification client.
 * Configured when FCM server key is provided.
 */
@Component
@RequiredArgsConstructor
public class FcmClient {

    private static final Logger log = LoggerFactory.getLogger(FcmClient.class);

    public boolean sendPush(String deviceToken, String title, String body) {
        if (deviceToken == null || deviceToken.isBlank()) {
            log.warn("FCM push skipped — no device token");
            return false;
        }
        // TODO: integrate Firebase Admin SDK when FCM server key is configured
        log.info("FCM push queued for token {}... title={}", deviceToken.substring(0, Math.min(10, deviceToken.length())), title);
        return true;
    }
}
