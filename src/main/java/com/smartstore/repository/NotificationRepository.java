package com.smartstore.repository;

import com.smartstore.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndUserTypeOrderByCreatedAtDesc(
        Long userId, String userType);
    long countByUserIdAndUserTypeAndIsRead(
        Long userId, String userType, boolean isRead);
    void deleteByUserIdAndUserType(Long userId, String userType);
    List<Notification> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}