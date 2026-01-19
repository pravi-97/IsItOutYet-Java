package com.p2.isitout.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
public class NotificationLog {

    @Id
    private UUID notificationId;

    private UUID userId;
    private UUID showId;

    private String notificationType;

    private String status; // PENDING | SENT | FAILED
    private int retryCount;
    private Instant lastAttemptAt;

    @CreationTimestamp
    private Instant triggeredAt;
}
