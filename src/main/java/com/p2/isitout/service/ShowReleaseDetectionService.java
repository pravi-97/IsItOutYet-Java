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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class ShowReleaseDetectionService {
    private final TvMazeClient tvMazeClient;
    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserShowRepository userShowRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void detectAndProcessNewReleases() {
        log.info("Starting ShowReleaseDetectionService on {} ", LocalDateTime.now());
        Instant start = Instant.now();
        Map<UUID, TvMazeShowDto> tvMazeMap = new HashMap<>();
        List<ShowReleaseState> showReleaseStates = showReleaseStateRepository.findAllByShow_StatusIn(List.of("Running", "To Be Determined", "In Development", "Planned"));

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

            if (apiData == null || apiData.getLinks() == null) {
                continue;
            }

            String apiStatus = apiData.getStatus();
            String dbStatus = state.getShow().getStatus();
            dbStatus = dbStatus == null ? "Running" : dbStatus;

            if (apiStatus != null && !apiStatus.equalsIgnoreCase(dbStatus)) {
                state.getShow().setStatus(apiStatus);
            }

            if (!isEndedStatus(dbStatus) && isEndedStatus(apiStatus)) {
                handleShowEnded(state);
                state.getShow().setStatus(apiStatus);
                state.setLastFetchedAt(Instant.now());
                showReleaseStateRepository.save(state);
                continue;
            }

            if (!isCancelledStatus(dbStatus) && isCancelledStatus(apiStatus)) {
                handleShowCancelled(state);
                state.getShow().setStatus(apiStatus);
                state.setLastFetchedAt(Instant.now());
                showReleaseStateRepository.save(state);
                continue;
            }

            if (!"Running".equalsIgnoreCase(apiStatus)) {
                state.setLastFetchedAt(Instant.now());
                showReleaseStateRepository.save(state);
                continue;
            }
            TvMazeShowDto.NextEpisode nextEpApi = apiData.getLinks().getNextepisode();
            if (nextEpApi == null) {
                if (state.getNextEpisodeNumber() != null) {
                    state.setNextSeasonNumber(null);
                    state.setNextEpisodeNumber(null);
                    state.setNextEpisodeAirDate(null);
                    state.setNextSeasonAirDate(null);
                    state.setLastFetchedAt(Instant.now());
                    showReleaseStateRepository.save(state);
                }
                continue;
            }

            EpisodeDetails epDetails = tvMazeClient.getDataFromTvMazeApi(
                    trimBaseUrl(nextEpApi.getHref()),
                    new HashMap<>(),
                    EpisodeDetails.class
            );

            Integer apiSeason = epDetails.getSeason();
            Integer apiEpisode = epDetails.getNumber();
            Instant episodeAirInstant = epDetails.getAirstamp() != null ? Instant.parse(epDetails.getAirstamp()) : null;
            Integer previousKnownSeason = state.getLastSeasonNumber();

            if (state.getCurrentSeasonNumber() == null) {
                state.setCurrentSeasonNumber(apiSeason);
                state.setLastNotifiedSeasonNumber(
                        resolveInitialLastNotifiedSeason(state, apiSeason, apiEpisode, episodeAirInstant, previousKnownSeason)
                );
            } else if (state.getLastNotifiedSeasonNumber() == null) {
                state.setLastNotifiedSeasonNumber(
                        resolveInitialLastNotifiedSeason(state, apiSeason, apiEpisode, episodeAirInstant, previousKnownSeason)
                );
            }

            if (apiSeason != null
                    && state.getCurrentSeasonNumber() != null
                    && apiSeason > state.getCurrentSeasonNumber()) {
                handleSeasonAnnouncedEarly(state, epDetails);
            }

            state.setCurrentSeasonNumber(apiSeason);
            Integer lastNotifiedSeason = state.getLastNotifiedSeasonNumber() == null
                    ? 0
                    : state.getLastNotifiedSeasonNumber();
            boolean seasonHasStarted = apiEpisode != null
                    && (apiEpisode > 1 || (episodeAirInstant != null && !episodeAirInstant.isAfter(Instant.now())));

            if (apiSeason != null && apiEpisode != null && apiSeason > lastNotifiedSeason && seasonHasStarted) {
                handleNewSeasonAnnouncement(state, epDetails);
                state.setLastNotifiedSeasonNumber(apiSeason);
            }

            state.setNextSeasonNumber(apiSeason);
            state.setNextEpisodeNumber(epDetails.getNumber());
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
            String idempotencyKey = String.format(
                    "SEASON_STARTED:%s:%d:%s",
                    state.getShowId(),
                    epDetails.getSeason(),
                    userShow.getUser().getUserId()
            );

            createLogIfNotExists(
                    userShow.getUser().getUserId(),
                    state.getShowId(),
                    NotificationType.SEASON_STARTED,
                    epDetails.getSeason(),
                    epDetails.getNumber(),
                    idempotencyKey
            );
        });
    }

    private void createLogIfNotExists(
            UUID userId,
            UUID showId,
            NotificationType notificationType,
            Integer season,
            Integer number,
            String idempotencyKey
    ) {
        if (notificationLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setNotificationId(UUID.randomUUID());
        notificationLog.setUserId(userId);
        notificationLog.setShowId(showId);
        notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
        notificationLog.setNotificationType(notificationType);
        notificationLog.setSeasonNumber(normalizeNotificationNumber(season));
        notificationLog.setEpisodeNumber(normalizeNotificationNumber(number));
        notificationLog.setRetryCount(0);
        notificationLog.setTriggeredAt(Instant.now());
        notificationLog.setIdempotencyKey(idempotencyKey);
        try {
            notificationLogRepository.save(notificationLog);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Skipping duplicate notification {}", idempotencyKey);
        }
    }

    private void handleSeasonAnnouncedEarly(ShowReleaseState state, EpisodeDetails epDetails) {
        List<UserShow> followers = userShowRepository.findByShow_ShowIdAndNotifySeasonConfirmedTrue(state.getShowId());
        followers.forEach(userShow -> {
            String key = String.format(
                    "SEASON_ANNOUNCED:%s:%d:%s",
                    state.getShowId(),
                    epDetails.getSeason(),
                    userShow.getUser().getUserId()
            );

            createLogIfNotExists(
                    userShow.getUser().getUserId(),
                    state.getShowId(),
                    NotificationType.SEASON_ANNOUNCED,
                    epDetails.getSeason(),
                    null,
                    key
            );
        });
    }

    private boolean isCancelledStatus(String status) {
        return status != null
                && (status.equalsIgnoreCase("Canceled") || status.equalsIgnoreCase("Cancelled"));
    }

    private boolean isEndedStatus(String status) {
        return status != null && status.equalsIgnoreCase("Ended");
    }

    private void handleShowEnded(ShowReleaseState state) {
        List<UserShow> followers = userShowRepository.findByShow_ShowId(state.getShowId());
        for (UserShow userShow : followers) {
            String key = String.format("SHOW_ENDED:%s:%s", state.getShowId(), userShow.getUser().getUserId());
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

    private int normalizeNotificationNumber(Integer value) {
        return value != null ? value : 0;
    }

    private int resolveInitialLastNotifiedSeason(
            ShowReleaseState state,
            Integer apiSeason,
            Integer apiEpisode,
            Instant episodeAirInstant,
            Integer previousKnownSeason
    ) {
        if (state.getLastNotifiedSeasonNumber() != null && state.getLastNotifiedSeasonNumber() > 0) {
            return state.getLastNotifiedSeasonNumber();
        }

        if (apiSeason == null) {
            return previousKnownSeason != null ? previousKnownSeason : 0;
        }

        boolean seasonStartsInFuture = apiEpisode != null
                && apiEpisode == 1
                && episodeAirInstant != null
                && episodeAirInstant.isAfter(Instant.now());

        if (seasonStartsInFuture) {
            return previousKnownSeason != null ? previousKnownSeason : 0;
        }

        return apiSeason;
    }
}
