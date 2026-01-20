
package com.p2.isitout.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity

@Table(name = "notification_log",uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key")
)
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
    private String idempotencyKey; //EPISODE_RELEASE:<showId>:<season>:<episode>:<userId>, SEASON_CONFIRMED:<showId>:<season>:<userId>

    public enum NotificationType {
        EPISODE_RELEASE,
        SEASON_CONFIRMED,
        DATE_CHANGED
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
