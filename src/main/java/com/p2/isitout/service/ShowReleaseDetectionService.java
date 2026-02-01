package com.p2.isitout.service;

import com.p2.isitout.client.tvmaze.TvMazeClient;
import com.p2.isitout.client.tvmaze.dto.EpisodeDetails;
import com.p2.isitout.client.tvmaze.dto.TvMazeShowDto;
import com.p2.isitout.entity.NotificationLog;
import com.p2.isitout.entity.NotificationType;
import com.p2.isitout.entity.ShowReleaseState;
import com.p2.isitout.entity.UserShow;
import com.p2.isitout.repository.NotificationLogRepository;
import com.p2.isitout.repository.ShowReleaseStateRepository;
import com.p2.isitout.repository.UserShowRepository;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ShowReleaseDetectionService {
    private final TvMazeClient tvMazeClient;
    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserShowRepository userShowRepository;
    private final NotificationLogRepository notificationLogRepository;

    public ShowReleaseDetectionService(TvMazeClient tvMazeClient, ShowReleaseStateRepository showReleaseStateRepository, UserShowRepository userShowRepository, NotificationLogRepository notificationLogRepository) {
        this.tvMazeClient = tvMazeClient;
        this.showReleaseStateRepository = showReleaseStateRepository;
        this.userShowRepository = userShowRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional
    public void detectAndProcessNewReleases() {
        log.info("Starting ShowReleaseDetectionService on {} ", LocalDateTime.now());
        Instant start = Instant.now();
        Map<UUID, TvMazeShowDto> tvMazeMap = new HashMap<>();
        List<ShowReleaseState> showReleaseStates = showReleaseStateRepository.findAllByShow_Status("Running");
        showReleaseStates.forEach(shows -> {
            TvMazeShowDto tvMazeShowDto = tvMazeClient.getShowByImdbId(shows.getShow().getImdbId());
            tvMazeMap.put(shows.getShowId(), tvMazeShowDto);
        });
        compareAndUpdate(tvMazeMap, showReleaseStates);
        log.info("Polling {} shows took {} ms", showReleaseStates.size(), Duration.between(start, Instant.now()).toMillis());
        log.info("Ending ShowReleaseDetectionService on {} ", LocalDateTime.now());
    }

    private void compareAndUpdate(Map<UUID, TvMazeShowDto> tvMazeMap, List<ShowReleaseState> dbStates) {
        for (ShowReleaseState state : dbStates) {
            TvMazeShowDto apiData = tvMazeMap.get(state.getShowId());

            // 1. Safety Check
            if (apiData == null || apiData.getLinks() == null)
                continue;

            String apiStatus = apiData.getStatus();
            String dbStatus  = state.getShow().getStatus();
            dbStatus = dbStatus == null ? "Running" : dbStatus;

            // ENDED DETECTION
            if (!isEndedStatus(dbStatus) && isEndedStatus(apiStatus)) {
                handleShowEnded(state);

                state.getShow().setStatus(apiStatus);
                state.setLastFetchedAt(Instant.now());
                showReleaseStateRepository.save(state);

                continue; // 🚨 stop episode/season logic
            }

            // CANCELLED DETECTION
            if (!isCancelledStatus(dbStatus) && isCancelledStatus(apiStatus)) {
                handleShowCancelled(state);

                state.getShow().setStatus(apiStatus);
                state.setLastFetchedAt(Instant.now());
                showReleaseStateRepository.save(state);

                continue; // 🚨 stop episode/season logic
            }

            TvMazeShowDto.NextEpisode nextEpApi = apiData.getLinks().getNextepisode();

            // 2. Scenario: API says "No Next Episode" (Show Ended or Waiting)
            if (nextEpApi == null) {
                if (state.getNextEpisodeNumber() != null) {
                    // Clear "Next" fields so we don't send false reminders
                    state.setNextSeasonNumber(null);
                    state.setNextEpisodeNumber(null);
                    state.setNextEpisodeAirDate(null);
                    state.setNextSeasonAirDate(null);
                    state.setLastFetchedAt(Instant.now());
                    showReleaseStateRepository.save(state);
                }
                continue;
            }

            // 3. Scenario: API has Next Episode -> Fetch Details
            EpisodeDetails epDetails = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(nextEpApi.getHref()), new HashMap<>(), EpisodeDetails.class);

            // --- CORE NOTIFICATION LOGIC ---
            Integer apiSeason = epDetails.getSeason();
            Integer apiEpisode = epDetails.getNumber();
            Instant episodeAirInstant = epDetails.getAirstamp() != null ? Instant.parse(epDetails.getAirstamp()) : null;

            // First sync → initialize, no notification
            if (state.getLastNotifiedSeasonNumber() == null) {
                state.setLastNotifiedSeasonNumber(apiSeason);
                state.setCurrentSeasonNumber(apiSeason);
            }

            // EARLY SEASON ANNOUNCEMENT
            if (apiSeason != null && state.getCurrentSeasonNumber() != null && apiSeason > state.getCurrentSeasonNumber()) {
                handleSeasonAnnouncedEarly(state, epDetails);
            }
            // Always track current season
            state.setCurrentSeasonNumber(apiSeason);
            // SEASON STARTED (episode 1 has aired)
            if (apiSeason != null && apiEpisode != null && apiEpisode == 1 && apiSeason > state.getLastNotifiedSeasonNumber() && episodeAirInstant != null && !episodeAirInstant.isAfter(Instant.now())) {
                handleNewSeasonAnnouncement(state, epDetails);
                state.setLastNotifiedSeasonNumber(apiSeason);
            }

            // 4. Update "Next" Fields (Used by your Scheduler for "6 hours before" emails)
            state.setNextSeasonNumber(apiSeason);
            state.setNextEpisodeNumber(epDetails.getNumber());

            // Parse Timestamps
            if (epDetails.getAirstamp() != null) {
                state.setNextEpisodeAirDate(Instant.parse(epDetails.getAirstamp()));
            }
            state.setLastFetchedAt(Instant.now());
            showReleaseStateRepository.save(state);
        }
    }

    private void handleNewSeasonAnnouncement(ShowReleaseState state, EpisodeDetails epDetails) {
        List<UserShow> followers = userShowRepository.findByShow_ShowIdAndNotifyNewSeasonTrue(state.getShowId());

        followers.forEach(userShow -> {
            // 2. Generate Idempotency Key (Safety Net)
            // Format: SEASON_ANNOUNCED:<ShowID>:<SeasonNumber>:<UserID>
            String idempotencyKey = String.format("SEASON_STARTED:%s:%d:%s", state.getShowId(), epDetails.getSeason(), userShow.getUser().getUserId());

            // 3. Queue Notification
            createLogIfNotExists(userShow.getUser().getUserId(), state.getShowId(), NotificationType.SEASON_STARTED, epDetails.getSeason(), epDetails.getNumber(), idempotencyKey);
        });
    }

    private void createLogIfNotExists(UUID userId, UUID showId, NotificationType notificationType, Integer season, Integer number, String idempotencyKey) {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setNotificationId(UUID.randomUUID());
        notificationLog.setUserId(userId);
        notificationLog.setShowId(showId);
        notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
        notificationLog.setNotificationType(notificationType);
        notificationLog.setSeasonNumber(season);
        notificationLog.setEpisodeNumber(number);
        notificationLog.setIdempotencyKey(idempotencyKey);
        notificationLogRepository.save(notificationLog);
    }

    private void handleSeasonAnnouncedEarly(ShowReleaseState state, EpisodeDetails epDetails) {
        List<UserShow> followers = userShowRepository.findByShow_ShowIdAndNotifySeasonConfirmedTrue(state.getShowId());

        followers.forEach(userShow -> {
            String key = String.format("SEASON_ANNOUNCED:%s:%d:%s", state.getShowId(), epDetails.getSeason(), userShow.getUser().getUserId());

            createLogIfNotExists(userShow.getUser().getUserId(), state.getShowId(), NotificationType.SEASON_ANNOUNCED, epDetails.getSeason(), null, key);
        });
    }

    private boolean isCancelledStatus(String status) {
        return status != null &&
                (status.equalsIgnoreCase("Canceled") ||
                        status.equalsIgnoreCase("Cancelled"));
    }

    private boolean isEndedStatus(String status) {
        return status != null &&
                status.equalsIgnoreCase("Ended");
    }
    private void handleShowEnded(ShowReleaseState state) {

        List<UserShow> followers = userShowRepository.findByShow_ShowId(state.getShowId());

        for (UserShow userShow : followers) {
            String key = String.format("SHOW_ENDED:%s:%s", state.getShowId(), userShow.getUser().getUserId()
            );

            createLogIfNotExists(
                    userShow.getUser().getUserId(),
                    state.getShowId(),
                    NotificationType.SHOW_ENDED,
                    null,
                    null,
                    key
            );
        }

        log.info("Show {} ended normally. Notifications queued: {}", state.getShowId(), followers.size());
    }
    private void handleShowCancelled(ShowReleaseState state) {
        List<UserShow> followers = userShowRepository.findByShow_ShowId(state.getShowId());
        for (UserShow userShow : followers) {

            String key = String.format("SHOW_CANCELLED:%s:%s", state.getShowId(), userShow.getUser().getUserId());

            createLogIfNotExists(
                    userShow.getUser().getUserId(),
                    state.getShowId(),
                    NotificationType.SHOW_CANCELLED,
                    null,
                    null,
                    key
            );
        }

        log.info("Show {} was cancelled. Notifications queued: {}", state.getShowId(), followers.size());
    }

    public static String trimBaseUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath();
    }
}