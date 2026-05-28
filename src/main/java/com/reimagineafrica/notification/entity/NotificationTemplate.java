package com.reimagineafrica.notification.entity;

import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationEvent;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationTemplate {

    @Id
    @Column(name = "id", length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 50)
    private NotificationEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "sw";

    @Column(name = "subject", length = 300)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Replace {{placeholder}} tokens with values from the map.
     */
    public String render(Map<String, String> params) {
        if (params == null || params.isEmpty()) return body;
        String rendered = body;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return rendered;
    }
}
