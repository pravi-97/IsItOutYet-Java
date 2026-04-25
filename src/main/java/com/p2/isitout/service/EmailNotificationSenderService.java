package com.p2.isitout.service;

import com.p2.isitout.client.notification.email.ZeptoMailEmailClient;
import com.p2.isitout.client.notification.email.dto.ZeptoMailRequest;
import com.p2.isitout.client.notification.email.dto.ZeptoMailResponse;
import com.p2.isitout.entity.*;
import com.p2.isitout.repository.*;
import com.p2.isitout.utils.ApplicationConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class EmailNotificationSenderService {

    private final ShowReleaseStateRepository showReleaseStateRepository;
    @Value("${notification.max.retries}")
    private Integer maxRetries;

    @Value("${zeptomail.from.address}")
    private String fromAddress;

    @Value("${zeptomail.from.name}")
    private String fromName;

    private final ZeptoMailEmailClient zeptoMailEmailClient;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendEmails(NotificationLog.NotificationStatus notificationStatus) {
        log.info("Starting EmailNotificationSenderService on {} ", LocalDateTime.now());
        EnumMap<NotificationType, EmailTemplate> emailTemplateMap = emailTemplateRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        EmailTemplate::getNotificationType,
                        Function.identity(),
                        (a, b) -> b,
                        () -> new EnumMap<>(NotificationType.class)
                ));
        List<NotificationLog> notificationLogList;
        notificationLogList = notificationLogRepository.findByStatusAndRetryCountLessThan(notificationStatus, maxRetries);

        List<UUID> showIds = notificationLogList.stream()
                .map(NotificationLog::getShowId)
                .distinct()
                .toList();

        Map<UUID, ShowReleaseState> showReleaseStateMap = showReleaseStateRepository.findAllById(showIds)
                .stream()
                .collect(Collectors.toMap(
                        ShowReleaseState::getShowId,
                        Function.identity()
                ));
        Map<UUID, User> users = fetchUserDetails(notificationLogList);
        notificationLogList.forEach(notificationLog -> {
            try {
                User user = users.get(notificationLog.getUserId());
                if (user == null) {
                    throw new IllegalStateException("User not found for notification " + notificationLog.getIdempotencyKey());
                }
                ZeptoMailRequest request = createMailRequest(user, notificationLog, emailTemplateMap, showReleaseStateMap);
                ZeptoMailResponse response = zeptoMailEmailClient.sendMail(request);
                notificationLog.setLastAttemptAt(Instant.now());
                if(response != null && response.data() != null && !response.data().isEmpty() &&
                        ApplicationConstants.ZEPTO_SUCCESS_CODE.equals(response.data().getFirst().code())){
                    log.info("Email sent successfully {}", notificationLog.getIdempotencyKey());
                    notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);
                    notificationLog.setSentAt(Instant.now());
                    notificationLog.setFailedAt(null);
                    notificationLog.setLastError(null);
                }else{
                    markNotificationFailed(notificationLog, "Email provider returned a non-success response");
                }
            } catch (Exception ex) {
                log.error("Email send failed: {}", notificationLog.getIdempotencyKey(), ex);
                markNotificationFailed(notificationLog, ex.getMessage());
            }
        });
        notificationLogRepository.saveAll(notificationLogList);
        log.info("Ending EmailNotificationSenderService on {} ", LocalDateTime.now());

    }

    private Map<UUID, User> fetchUserDetails(List<NotificationLog> notificationLogList) {
        List<UUID> userIds = notificationLogList.stream()
                .map(NotificationLog::getUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        Function.identity()
                ));
    }

    private ZeptoMailRequest createMailRequest(User user, NotificationLog notificationLog, Map<NotificationType, EmailTemplate> emailTemplateMap, Map<UUID, ShowReleaseState> showReleaseStateMap) {
        String userName = user.getName();
        if(null == userName || userName.isEmpty()){
            userName = user.getUsername();
        }
        Map<String, String> emailParams = getEmailParams(notificationLog, userName, emailTemplateMap, showReleaseStateMap);
        return new ZeptoMailRequest(new ZeptoMailRequest
                .From(fromAddress, fromName), List.of(new ZeptoMailRequest
                .To(new ZeptoMailRequest
                .EmailAddress(user.getEmail(), user.getName()))),
                emailParams.get("subject"), emailParams.get("body"));
    }

    private Map<String, String> getEmailParams(NotificationLog notificationLog, String userName, Map<NotificationType, EmailTemplate> emailTemplateMap, Map<UUID, ShowReleaseState> showReleaseStateMap) {
        Map<String, String> emailParams = new HashMap<>();
        ShowReleaseState showReleaseState = showReleaseStateMap.get(notificationLog.getShowId());
        if(showReleaseState == null){
            throw new RuntimeException("Show release state not found");
        }

        EmailTemplate emailTemp = emailTemplateMap.get(notificationLog.getNotificationType());
        if(emailTemp == null){
            throw new RuntimeException("Email template not found");
        }
        emailParams.put("subject", emailTemp.getSubject().replace(ApplicationConstants.EPH_SHOW_NAME, showReleaseState.getShowName()));
        String emailBody = emailTemp.getBody();
        emailBody = emailBody.replace(ApplicationConstants.EPH_USER_NAME, userName)
                .replace(ApplicationConstants.EPH_SEASON, formatNumber(notificationLog.getSeasonNumber()))
                .replace(ApplicationConstants.EPH_EPISODE, formatNumber(notificationLog.getEpisodeNumber()))
                .replace(ApplicationConstants.EPH_SHOW_NAME, showReleaseState.getShowName())
                .replace(ApplicationConstants.EPH_AIR_DATE, formatInstant(showReleaseState.getNextEpisodeAirDate()));
        emailParams.put("body", emailBody);
        return emailParams;
    }

    private String formatNumber(Integer value) {
        return value == null || value <= 0 ? "" : String.valueOf(value);
    }

    private String formatInstant(Instant value) {
        return value != null ? value.toString() : "";
    }

    private void markNotificationFailed(NotificationLog notificationLog, String message) {
        notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
        notificationLog.setRetryCount(notificationLog.getRetryCount() != null ? notificationLog.getRetryCount() + 1 : 1);
        notificationLog.setFailedAt(Instant.now());
        notificationLog.setLastError(message);
    }
}
