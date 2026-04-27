package com.p2.isitout.repository;

import com.p2.isitout.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT n
            FROM NotificationLog n
            WHERE n.status = :status
              AND COALESCE(n.retryCount, 0) < :maxRetries
            """)
    List<NotificationLog> findByStatusAndRetryCountLessThan(
            @Param("status") NotificationLog.NotificationStatus status,
            @Param("maxRetries") Integer retryCountIsLessThan
    );
}
