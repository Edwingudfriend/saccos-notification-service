package com.reimagineafrica.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationTemplate {

    @Id
    @Column(name = "id", updatable = false, length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "language", nullable = false, length = 5)
    @Builder.Default
    private String language = "SW";

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "template", nullable = false, columnDefinition = "TEXT")
    private String template;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Fills template placeholders with actual values.
     * Template: "Karibu {name}! Namba yako ni {memberNumber}"
     * Variables: {name="John", memberNumber="RA-2026-0001"}
     */
    public String render(java.util.Map<String, String> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
