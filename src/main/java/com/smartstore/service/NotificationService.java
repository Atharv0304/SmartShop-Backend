package com.smartstore.service;

import com.smartstore.model.Notification;
import com.smartstore.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(Long userId, String userType,
            String type, String title, String message,
            Long orderId, String otp) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setUserType(userType);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setOrderId(orderId);
        notif.setOtp(otp);
        notif.setRead(false);
        notif.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notif);
    }

    public List<Notification> getNotifications(Long userId, String userType) {
        return notificationRepository
            .findByUserIdAndUserTypeOrderByCreatedAtDesc(userId, userType);
    }

    public long getUnreadCount(Long userId, String userType) {
        return notificationRepository
            .countByUserIdAndUserTypeAndIsRead(userId, userType, false);
    }

    public void markAllRead(Long userId, String userType) {
        List<Notification> notifs = notificationRepository
            .findByUserIdAndUserTypeOrderByCreatedAtDesc(userId, userType);
        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
    }

    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}