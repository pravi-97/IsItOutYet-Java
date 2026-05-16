
package com.p2.isitout.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity

@Table(name = "notification_log",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"),
        indexes = {@Index(name = "idx_notification_status", columnList = "status")})
@Getter
@Setter
public class NotificationLog {

    @Id
    private UUID notificationId;

    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private UUID showId;

    @Column(nullable = false)
    private Integer seasonNumber;
    @Column(nullable = false)
    private Integer episodeNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Integer retryCount;

    @Column(nullable = false)
    private Instant triggeredAt;

    private Instant lastAttemptAt;
    private Instant sentAt;
    private Instant failedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey; //NotificationType:<showId>:<season>:<episode>:<userId>,

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
