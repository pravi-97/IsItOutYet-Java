package com.p2.isitout.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class NotificationLogDto {

    private UUID notificationId;

    private UUID userId;
    private UUID showId;

    private String notificationType;

    private String status; // PENDING | SENT | FAILED
    private int retryCount;
    private Instant lastAttemptAt;

    private Instant triggeredAt;
}
