package com.reimagineafrica.notification.repository;

import com.reimagineafrica.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByEventTypeAndChannelAndLanguageAndActiveTrue(
            String eventType, String channel, String language);

    // Fallback to English if Swahili template not found
    default Optional<NotificationTemplate> findTemplate(String eventType, String channel, String language) {
        return findByEventTypeAndChannelAndLanguageAndActiveTrue(eventType, channel, language)
                .or(() -> findByEventTypeAndChannelAndLanguageAndActiveTrue(eventType, channel, "EN"));
    }
}
