package com.p2.isitout.repository;

import com.p2.isitout.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {

    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationLog.NotificationStatus status, Integer retryCountIsLessThan);
}
