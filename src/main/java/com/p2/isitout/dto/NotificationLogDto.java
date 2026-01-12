package com.p2.isitout.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
public class NotificationLogDto {

    @Id
    private UUID notificationId;

    private UUID userId;
    private UUID showId;

    private String notificationType;

    private String status; // PENDING | SENT | FAILED
    private int retryCount;
    private Instant lastAttemptAt;

    private Instant triggeredAt;
}
