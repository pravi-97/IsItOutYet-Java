package com.p2.isitout.repository;

import com.p2.isitout.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {
    void createLogIfNotExists(UUID userId, UUID showId, NotificationLog.NotificationType notificationType, Integer season, Integer number, String idempotencyKey);
}
