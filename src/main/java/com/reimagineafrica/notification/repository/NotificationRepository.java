package com.reimagineafrica.notification.repository;

import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);
    Page<Notification> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status IN ('PENDING','RETRYING') AND n.attempts < n.maxAttempts")
    List<Notification> findRetryable();

    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
