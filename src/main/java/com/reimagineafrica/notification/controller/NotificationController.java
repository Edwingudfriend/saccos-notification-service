package com.reimagineafrica.notification.controller;

import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','AUDITOR')")
    public ResponseEntity<Page<Notification>> byMember(
            @PathVariable String memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByMember(memberId, pageable));
    }

    @GetMapping("/reference/{referenceId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','AUDITOR')")
    public ResponseEntity<Page<Notification>> byReference(
            @PathVariable String referenceId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByReference(referenceId, pageable));
    }
}
