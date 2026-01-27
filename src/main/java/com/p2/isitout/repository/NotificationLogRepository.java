package com.p2.isitout.repository;

import com.p2.isitout.entity.NotificationLog;
import com.p2.isitout.entity.NotificationType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {

    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
