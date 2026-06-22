package com.smartattendance.repository;

import com.smartattendance.entity.AppNotification;
import com.smartattendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByUserOrderByCreatedAtDesc(User user);

    List<AppNotification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);
}
