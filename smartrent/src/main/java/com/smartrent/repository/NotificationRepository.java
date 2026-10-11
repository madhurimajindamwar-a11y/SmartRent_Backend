package com.smartrent.repository;

import com.smartrent.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipientIdOrderByIdDesc(
            Long recipientId
    );

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    boolean existsByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :now
            WHERE n.id = :id
              AND n.recipientId = :recipientId
              AND n.readAt IS NULL
            """)
    int markRead(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :now
            WHERE n.recipientId = :recipientId
              AND n.readAt IS NULL
            """)
    int markAllRead(
            @Param("recipientId") Long recipientId,
            @Param("now") LocalDateTime now
    );
}