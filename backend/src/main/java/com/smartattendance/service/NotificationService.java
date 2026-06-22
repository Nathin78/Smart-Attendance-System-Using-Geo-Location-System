package com.smartattendance.service;

import com.smartattendance.dto.NotificationResponse;
import com.smartattendance.entity.AppNotification;
import com.smartattendance.entity.User;
import com.smartattendance.repository.NotificationRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUser(String email, String title, String message, String category, String linkUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        saveNotification(user, title, message, category, linkUrl);
    }

    public void notifyAllUsers(String title, String message, String category, String linkUrl) {
        userRepository.findAll().forEach(user -> saveNotification(user, title, message, category, linkUrl));
    }

    public List<NotificationResponse> getUnread(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user).stream()
                .map(this::map)
                .toList();
    }

    public List<NotificationResponse> getAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::map)
                .toList();
    }

    public NotificationResponse markRead(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        AppNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification not found");
        }
        notification.setReadAt(LocalDateTime.now());
        return map(notificationRepository.save(notification));
    }

    private AppNotification saveNotification(User user, String title, String message, String category, String linkUrl) {
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setLinkUrl(linkUrl);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    private NotificationResponse map(AppNotification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setCategory(notification.getCategory());
        response.setLinkUrl(notification.getLinkUrl());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        return response;
    }
}
