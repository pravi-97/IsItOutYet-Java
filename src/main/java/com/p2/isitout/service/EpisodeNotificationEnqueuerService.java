package com.p2.isitout.service;

import com.google.common.collect.Lists;
import com.p2.isitout.dao.NotificationLogDao;
import com.p2.isitout.entity.NotificationLog;
import com.p2.isitout.entity.NotificationType;
import com.p2.isitout.entity.ShowReleaseState;
import com.p2.isitout.entity.UserShow;
import com.p2.isitout.repository.NotificationLogRepository;
import com.p2.isitout.repository.ShowReleaseStateRepository;
import com.p2.isitout.repository.UserShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service@RequiredArgsConstructor
public class EpisodeNotificationEnqueuerService {

    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserShowRepository userShowRepository;
    private final NotificationLogDao notificationLogDao;

    @Transactional
    public void enqueue() {
        log.info("Starting EpisodeNotificationEnqueuerService on {} ", LocalDateTime.now());
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tomorrow = today.plusDays(1);
        List<NotificationLog> notificationLogsList = new ArrayList<>();

        List < ShowReleaseState > states = showReleaseStateRepository.findStatesWithNextEpisode();

        for (ShowReleaseState state: states) {
            Instant airTime = state.getNextEpisodeAirDate();
            if (airTime == null) continue;

            LocalDate airDate = airTime.atZone(ZoneOffset.UTC).toLocalDate();

            Integer season = state.getNextSeasonNumber();
            Integer episode = state.getNextEpisodeNumber();

            List < UserShow > users = userShowRepository.findByShow_ShowIdAndIsTrackingTrueAndNotifyNewEpisodeTrue(state.getShowId());

            for (UserShow userShow: users) {
                UUID userId = userShow.getUser().getUserId();

                // EPISODE_RELEASE
                if (!airTime.isAfter(now) && userShow.isNotifyEpisodeAfterRelease()){
                    NotificationLog notificationLog = enqueueNotification(NotificationType.EPISODE_RELEASE, state.getShowId(), season, episode, userId, now);
                    notificationLogsList.add(notificationLog);
                    }

                // REMINDER_TODAY
                if (airDate.equals(today) && airTime.isAfter(now) && userShow.isNotifyEpisodeOnReleaseDay()) {
                    NotificationLog notificationLog = enqueueNotification(NotificationType.REMINDER_TODAY, state.getShowId(), season, episode, userId, now);
                    notificationLogsList.add(notificationLog);
                }

                // REMINDER_TOMORROW
                if (airDate.equals(tomorrow) && userShow.isNotifyEpisodeDayBefore()) {
                    NotificationLog notificationLog = enqueueNotification(NotificationType.REMINDER_TOMORROW, state.getShowId(), season, episode, userId, now);
                    notificationLogsList.add(notificationLog);
                }
                if(notificationLogsList.size() >= 1000){
                    notificationLogDao.bulkInsertIgnore(notificationLogsList);
                    notificationLogsList.clear();
                }
            }
        }
        batchInsertNotificationLogs(notificationLogsList);
        log.info("Ending EpisodeNotificationEnqueuerService on {} ", LocalDateTime.now());
    }

    private void batchInsertNotificationLogs(List<NotificationLog> notificationLogsList) {
        final int BATCH_SIZE = 1000;
        List<List<NotificationLog>> chunks = Lists.partition(notificationLogsList, BATCH_SIZE);

        for (List<NotificationLog> chunk : chunks) {
            notificationLogDao.bulkInsertIgnore(chunk);
        }
    }

    private NotificationLog enqueueNotification(NotificationType type, UUID showId, Integer season, Integer episode, UUID userId, Instant now) {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setNotificationId(UUID.randomUUID());
        notificationLog.setUserId(userId);
        notificationLog.setShowId(showId);
        notificationLog.setSentAt(Instant.now());
        notificationLog.setSeasonNumber(season);
        notificationLog.setEpisodeNumber(episode);
        notificationLog.setNotificationType(type);
        notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
        notificationLog.setRetryCount(0);
        notificationLog.setTriggeredAt(now);
        notificationLog.setIdempotencyKey(buildKey(type, showId, season, episode, userId));
        return notificationLog;
    }

    private String buildKey(
            NotificationType type, UUID showId, Integer season, Integer episode, UUID userId) {
        return String.format("%s:%s:%d:%d:%s", type.name(), showId, season, episode, userId);
    }
}