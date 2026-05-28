package com.reimagineafrica.notification.controller;

import com.reimagineafrica.notification.entity.Notification;
import com.reimagineafrica.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Query notification history and resend")
public class NotificationController {

    private final NotificationService notifService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notification history")
    public ResponseEntity<Page<Notification>> myNotifications(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        String userId = (String) request.getAttribute("userId");
        return ResponseEntity.ok(notifService.getForRecipient(userId, pageable));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','AUDITOR')")
    @Operation(summary = "Get notification history for a specific member")
    public ResponseEntity<Page<Notification>> memberNotifications(
            @PathVariable String memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notifService.getForRecipient(memberId, pageable));
    }

    @GetMapping("/reference/{type}/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','AUDITOR')")
    @Operation(summary = "Get all notifications for a reference (e.g. LOAN/uuid)")
    public ResponseEntity<List<Notification>> referenceNotifications(
            @PathVariable String type,
            @PathVariable String id) {
        return ResponseEntity.ok(notifService.getForReference(type, id));
    }
}
