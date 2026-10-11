package com.smartrent.controller;

import com.smartrent.service.NotificationService;
import com.smartrent.service.NotificationService.NotificationSummary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationSummary getMine() {
        return notificationService.getMine();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable("id") Long id) {

        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}