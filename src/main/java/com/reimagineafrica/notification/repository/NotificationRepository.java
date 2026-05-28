package com.reimagineafrica.notification.repository;

import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(String memberId, Pageable pageable);
    Page<Notification> findByReferenceIdOrderByCreatedAtDesc(String referenceId, Pageable pageable);
    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
