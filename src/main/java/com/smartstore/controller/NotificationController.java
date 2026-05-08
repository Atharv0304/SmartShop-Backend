package com.smartstore.controller;

import com.smartstore.model.Notification;
import com.smartstore.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/{userId}/{userType}")
    public List<Notification> getNotifications(
            @PathVariable Long userId,
            @PathVariable String userType) {
        return notificationService.getNotifications(userId, userType);
    }

    @GetMapping("/count/{userId}/{userType}")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable Long userId,
            @PathVariable String userType) {
        return ResponseEntity.ok(Map.of(
            "count", notificationService.getUnreadCount(userId, userType)
        ));
    }

    @PutMapping("/read-all/{userId}/{userType}")
    public ResponseEntity<?> markAllRead(
            @PathVariable Long userId,
            @PathVariable String userType) {
        notificationService.markAllRead(userId, userType);
        return ResponseEntity.ok("All marked as read");
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok("Marked as read");
    }
}
