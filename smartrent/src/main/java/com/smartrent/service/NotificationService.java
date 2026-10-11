package com.smartrent.service;

import com.smartrent.entity.Notification;
import com.smartrent.entity.User;
import com.smartrent.repository.NotificationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(
            NotificationRepository notificationRepository,
            CurrentUserService currentUserService) {

        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public record NotificationView(
            Long id,
            String title,
            String message,
            String targetPath,
            Instant createdAt,
            boolean read
    ) {
    }

    public record NotificationSummary(
            long unreadCount,
            List<NotificationView> notifications
    ) {
    }

    // Called only by server-side business services.
    // Requires their existing transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(
            Long recipientId,
            String title,
            String message,
            String targetPath) {

        if (recipientId == null) {
            return;
        }

        notificationRepository.save(new Notification(
                recipientId,
                title,
                message,
                targetPath
        ));
    }

    @Transactional(readOnly = true)
    public NotificationSummary getMine() {
        User user = currentUserService.getCurrentUser();

        List<NotificationView> notifications = notificationRepository
                .findTop50ByRecipientIdOrderByIdDesc(user.getId())
                .stream()
                .map(notification -> new NotificationView(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getTargetPath(),
                        notification.getCreatedAt()
                                .toInstant(ZoneOffset.UTC),
                        notification.getReadAt() != null
                ))
                .toList();

        return new NotificationSummary(
                notificationRepository
                        .countByRecipientIdAndReadAtIsNull(user.getId()),
                notifications
        );
    }

    @Transactional
    public void markRead(Long id) {
        User user = currentUserService.getCurrentUser();

        if (!notificationRepository.existsByIdAndRecipientId(
                id, user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Notification not found"
            );
        }

        notificationRepository.markRead(
                id,
                user.getId(),
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }

    @Transactional
    public void markAllRead() {
        User user = currentUserService.getCurrentUser();

        notificationRepository.markAllRead(
                user.getId(),
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }
}