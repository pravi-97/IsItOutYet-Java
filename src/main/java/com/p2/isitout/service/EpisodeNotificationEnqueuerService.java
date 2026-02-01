package com.p2.isitout.service;

import com.p2.isitout.entity.NotificationLog;
import com.p2.isitout.entity.NotificationType;
import com.p2.isitout.entity.ShowReleaseState;
import com.p2.isitout.entity.UserShow;
import com.p2.isitout.repository.NotificationLogRepository;
import com.p2.isitout.repository.ShowReleaseStateRepository;
import com.p2.isitout.repository.UserShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service@RequiredArgsConstructor
public class EpisodeNotificationEnqueuerService {

    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserShowRepository userShowRepository;
    private final NotificationLogRepository notificationLogRepository;

    public void enqueue() {
        log.info("Starting EpisodeNotificationEnqueuerService on {} ", LocalDateTime.now());
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tomorrow = today.plusDays(1);

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

                // 1️⃣ EPISODE_RELEASE
                if (!airTime.isAfter(now) && userShow.isNotifyEpisodeOnReleaseDay()){
                        enqueueNotification(
                                NotificationType.EPISODE_RELEASE, state.getShowId(), season, episode, userId, now);
                    }

                // 2️⃣ REMINDER_TODAY
                if (airDate.equals(today) && airTime.isAfter(now) && userShow.isNotifyEpisodeOnReleaseDay()) {
                    enqueueNotification(
                            NotificationType.REMINDER_TODAY, state.getShowId(), season, episode, userId, now);
                }

                // 3️⃣ REMINDER_TOMORROW
                if (airDate.equals(tomorrow) && userShow.isNotifyEpisodeDayBefore()) {
                    enqueueNotification(
                            NotificationType.REMINDER_TOMORROW, state.getShowId(), season, episode, userId, now);
                }
            }
        }
        log.info("Ending EpisodeNotificationEnqueuerService on {} ", LocalDateTime.now());
    }

    private void enqueueNotification(
            NotificationType type, UUID showId, Integer season, Integer episode, UUID userId, Instant now) {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setNotificationId(UUID.randomUUID());
        notificationLog.setUserId(userId);
        notificationLog.setShowId(showId);
        notificationLog.setSeasonNumber(season);
        notificationLog.setEpisodeNumber(episode);
        notificationLog.setNotificationType(type);
        notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
        notificationLog.setRetryCount(0);
        notificationLog.setTriggeredAt(now);
        notificationLog.setIdempotencyKey(buildKey(type, showId, season, episode, userId));

        // DB unique constraint guarantees idempotency
        if(!notificationLogRepository.existsByIdempotencyKey(notificationLog.getIdempotencyKey())){
            notificationLogRepository.save(notificationLog);
        }else{
            log.error("Skip this because IdempotencyKey already exists {}",notificationLog.getIdempotencyKey());
        }
    }

    private String buildKey(
            NotificationType type, UUID showId, Integer season, Integer episode, UUID userId) {
        return String.format("%s:%s:%d:%d:%s", type.name(), showId, season, episode, userId);
    }
}