package com.reimagineafrica.notification.repository;

import com.reimagineafrica.notification.entity.NotificationTemplate;
import com.reimagineafrica.notification.enums.NotificationChannel;
import com.reimagineafrica.notification.enums.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByEventAndChannelAndLanguageAndActiveTrue(
            NotificationEvent event, NotificationChannel channel, String language);
}
