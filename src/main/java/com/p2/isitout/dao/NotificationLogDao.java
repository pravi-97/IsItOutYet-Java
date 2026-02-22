package com.p2.isitout.dao;

import com.p2.isitout.entity.NotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationLogDao {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertIgnore(List<NotificationLog> notifications) {
        String sql = """
            INSERT INTO notification_log
            (notification_id, user_id, show_id, season_number, episode_number, notification_type, status, idempotency_key, triggered_at, sent_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (idempotency_key) DO NOTHING
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                NotificationLog n = notifications.get(i);
                ps.setObject(1, n.getNotificationId());
                ps.setObject(2, n.getUserId());
                ps.setObject(3, n.getShowId());
                ps.setObject(4, n.getSeasonNumber());
                ps.setObject(5, n.getEpisodeNumber());
                ps.setString(6, n.getNotificationType().name());
                ps.setString(7, n.getStatus().name());
                ps.setString(8, n.getIdempotencyKey());
                ps.setObject(9, n.getTriggeredAt());
                ps.setObject(10, n.getSentAt());
            }

            @Override
            public int getBatchSize() {
                return notifications.size();
            }
        });
    }
}