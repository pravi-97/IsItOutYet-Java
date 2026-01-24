
package com.p2.isitout.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity

@Table(name = "notification_log",uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
@Getter
@Setter
public class NotificationLog {

    @Id
    private UUID notificationId;


    private UUID userId;
    private UUID showId;

    private Integer seasonNumber;
    private Integer episodeNumber;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private int retryCount;

    private Instant triggeredAt;
    private Instant lastAttemptAt;
    private Instant sentAt;
    private Instant failedAt;

    @Lob
    private String lastError;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey; //NotificationType:<showId>:<season>:<episode>:<userId>,

    public enum NotificationType {
        EPISODE_RELEASE,
        DATE_CHANGED,
        REMINDER_TODAY,
        REMINDER_TOMORROW,
        SEASON_ANNOUNCED,
        SEASON_STARTED,
        SHOW_CANCELLED,
        SHOW_ENDED
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
