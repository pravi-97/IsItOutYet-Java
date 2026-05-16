package com.p2.isitout.service;

import com.p2.isitout.client.notification.email.ZeptoMailEmailClient;
import com.p2.isitout.client.notification.email.dto.ZeptoMailRequest;
import com.p2.isitout.client.notification.email.dto.ZeptoMailResponse;
import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.client.tvmaze.TvMazeClient;
import com.p2.isitout.client.tvmaze.dto.TvMazeEpisodeDto;
import com.p2.isitout.entity.*;
import com.p2.isitout.repository.*;
import com.p2.isitout.utils.ApplicationConstants;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationSenderService {

    @Value("${notification.max.retries}")
    private Integer maxRetries;

    @Value("${zeptomail.from.address}")
    private String fromAddress;

    @Value("${zeptomail.from.name}")
    private String fromName;

    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final ZeptoMailEmailClient zeptoMailEmailClient;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final UserRepository userRepository;
    private final TmdbClient tmdbClient;
    private final TvMazeClient tvMazeClient;

    @Transactional
    public void sendEmails(NotificationLog.NotificationStatus notificationStatus) {
        log.info("Starting EmailNotificationSenderService on {}", LocalDateTime.now());
        Map<Integer, TvMazeEpisodeDto> showCache = new HashMap<>();
        Map<String, String> bannerUrls = new HashMap<>();
        Map<String, String> showLinkMap = new HashMap<>();

        EnumMap<NotificationType, EmailTemplate> emailTemplateMap = emailTemplateRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        EmailTemplate::getNotificationType,
                        Function.identity(),
                        (a, b) -> b,
                        () -> new EnumMap<>(NotificationType.class)
                ));

        List<NotificationLog> notificationLogList =
                notificationLogRepository.findByStatusAndRetryCountLessThan(notificationStatus, maxRetries);

        List<UUID> showIds = notificationLogList.stream()
                .map(NotificationLog::getShowId)
                .distinct()
                .toList();

        Map<UUID, ShowReleaseState> showReleaseStateMap = showReleaseStateRepository.findAllById(showIds)
                .stream()
                .collect(Collectors.toMap(ShowReleaseState::getShowId, Function.identity()));

        Map<UUID, User> users = fetchUserDetails(notificationLogList);

        notificationLogList.forEach(notificationLog -> {
            try {
                User user = users.get(notificationLog.getUserId());
                if (user == null) {
                    throw new IllegalStateException("User not found for notification " + notificationLog.getIdempotencyKey());
                }
                ZeptoMailRequest request = createMailRequest(user, notificationLog, emailTemplateMap, showReleaseStateMap, showCache, bannerUrls, showLinkMap);
                ZeptoMailResponse response = zeptoMailEmailClient.sendMail(request);

                notificationLog.setLastAttemptAt(Instant.now());

                if (response != null && response.data() != null && !response.data().isEmpty() &&
                        ApplicationConstants.ZEPTO_SUCCESS_CODE.equals(response.data().getFirst().code())) {
                    log.info("Email sent successfully {}", notificationLog.getIdempotencyKey());
                    notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);
                    notificationLog.setSentAt(Instant.now());
                    notificationLog.setFailedAt(null);
                    notificationLog.setLastError(null);
                } else {
                    markNotificationFailed(notificationLog, "Email provider returned a non-success response");
                }
            } catch (Exception ex) {
                log.error("Email send failed: {}", notificationLog.getIdempotencyKey(), ex);
                markNotificationFailed(notificationLog, ex.getMessage());
            }
        });

        saveNotificationLogs(notificationLogList);
        log.info("Ending EmailNotificationSenderService on {}", LocalDateTime.now());
    }

    public void saveNotificationLogs(List<NotificationLog> logs) {
        notificationLogRepository.saveAll(logs);
    }

    private Map<UUID, User> fetchUserDetails(List<NotificationLog> notificationLogList) {
        List<UUID> userIds = notificationLogList.stream().map(NotificationLog::getUserId).distinct().toList();
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    private ZeptoMailRequest createMailRequest(User user, NotificationLog notificationLog, Map<NotificationType, EmailTemplate> emailTemplateMap,
                                               Map<UUID, ShowReleaseState> showReleaseStateMap, Map<Integer, TvMazeEpisodeDto> showCache,
                                               Map<String, String> bannerUrls, Map<String, String> showLinkMap) {
        String userName = (user.getName() == null || user.getName().isEmpty()) ? user.getUsername() : user.getName();
        Map<String, String> emailParams = getEmailParams(notificationLog, userName, emailTemplateMap, showReleaseStateMap, user.getTimeZone(), showCache, bannerUrls, showLinkMap);
        return new ZeptoMailRequest(
                new ZeptoMailRequest.From(fromAddress, fromName),
                List.of(new ZeptoMailRequest.To(new ZeptoMailRequest.EmailAddress(user.getEmail(), user.getName()))),
                emailParams.get("subject"),
                emailParams.get("body")
        );
    }

    private Map<String, String> getEmailParams(NotificationLog notificationLog, String userName, Map<NotificationType, EmailTemplate> emailTemplateMap, Map<UUID, ShowReleaseState> showReleaseStateMap, String timeZone, Map<Integer, TvMazeEpisodeDto> showCache, Map<String, String> bannerUrls, Map<String, String> showLinkMap) {
        ShowReleaseState showReleaseState = showReleaseStateMap.get(notificationLog.getShowId());
        if (showReleaseState == null) throw new RuntimeException("Show release state not found");

        EmailTemplate emailTemp = emailTemplateMap.get(notificationLog.getNotificationType());
        if (emailTemp == null) throw new RuntimeException("Email template not found");

        TvMazeEpisodeDto tvMazeShowDetails = new TvMazeEpisodeDto();
        NotificationType notificationType =  notificationLog.getNotificationType();
        if(!notificationType.equals(NotificationType.SHOW_CANCELLED) && !notificationType.equals(NotificationType.SHOW_ENDED)){
            tvMazeShowDetails = getShowDetails(showReleaseState.getShow().getTvmazeId(), notificationLog.getSeasonNumber(), notificationLog.getEpisodeNumber(), showCache);
        }
        String tmdbId = showReleaseState.getShow().getTmdbId();
        ensureTmdbDataCached(tmdbId, bannerUrls, showLinkMap);

        String subject = emailTemp.getSubject().replace(ApplicationConstants.EPH_SHOW_NAME, showReleaseState.getShowName());
        String emailBody = emailTemp.getBody()
                .replace(ApplicationConstants.EPH_USER_NAME, userName)
                .replace(ApplicationConstants.EPH_SEASON, formatNumber(notificationLog.getSeasonNumber()))
                .replace(ApplicationConstants.EPH_EPISODE, formatNumber(notificationLog.getEpisodeNumber()))
                .replace(ApplicationConstants.EPH_SHOW_NAME, showReleaseState.getShowName())
                .replace("{{formattedAirDate}}", formatAirDate(tvMazeShowDetails.getAirstamp(), timeZone))
                .replace("{{episodeDescription}}", tvMazeShowDetails.getSummary() != null ? tvMazeShowDetails.getSummary() : "")
                .replace("{{bannerUrl}}", bannerUrls.get(tmdbId))
                .replace("{{episodeTitle}}", tvMazeShowDetails.getName() != null ? tvMazeShowDetails.getName() : "")
                .replace("{{showUrl}}", showLinkMap.get(tmdbId));
        return Map.of("subject", subject, "body", emailBody);
    }

    private void ensureTmdbDataCached(String tmdbId, Map<String, String> bannerUrls, Map<String, String> showLinkMap) {
        if (!bannerUrls.containsKey(tmdbId)) {
            TmdbTvShowDetailsDto tmdbShowDetails = tmdbClient.getTvShowById(tmdbId);
            bannerUrls.put(tmdbId, "https://image.tmdb.org/t/p/original" + tmdbShowDetails.getBackdropPath());
            showLinkMap.put(tmdbId, tmdbShowDetails.getHomepage());
        }
    }

    private TvMazeEpisodeDto getShowDetails(Integer tvMazeId, Integer seasonNumber, Integer episodeNumber,
                                            Map<Integer, TvMazeEpisodeDto> showCache) {
        int compositeKey = Objects.hash(tvMazeId, seasonNumber, episodeNumber);
        return showCache.computeIfAbsent(compositeKey,
                k -> tvMazeClient.getEpisodeBySeasonAndNumber(Long.valueOf(tvMazeId), seasonNumber, episodeNumber));
    }

    private String formatAirDate(String airStamp, String timeZone) {
        OffsetDateTime utcDateTime = OffsetDateTime.parse(airStamp);
        ZoneId zoneId = (timeZone == null || timeZone.isBlank()) ? ZoneOffset.UTC : ZoneId.of(timeZone);
        return utcDateTime.atZoneSameInstant(zoneId)
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a z"));
    }

    private String formatNumber(Integer value) {
        return value == null || value <= 0 ? "" : String.valueOf(value);
    }

    private void markNotificationFailed(NotificationLog notificationLog, String message) {
        notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
        notificationLog.setRetryCount(notificationLog.getRetryCount() != null ? notificationLog.getRetryCount() + 1 : 1);
        notificationLog.setFailedAt(Instant.now());
        notificationLog.setLastError(message);
    }
}