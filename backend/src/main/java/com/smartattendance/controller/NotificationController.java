package com.smartattendance.controller;

import com.smartattendance.dto.NotificationResponse;
import com.smartattendance.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getUnread(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getAll(authentication.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markRead(authentication.getName(), id));
    }
}
