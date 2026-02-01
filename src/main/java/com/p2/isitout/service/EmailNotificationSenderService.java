package com.p2.isitout.service;

import com.p2.isitout.client.notification.email.ZeptoMailEmailClient;
import com.p2.isitout.client.notification.email.dto.ZeptoMailRequest;
import com.p2.isitout.client.notification.email.dto.ZeptoMailResponse;
import com.p2.isitout.entity.*;
import com.p2.isitout.repository.*;
import com.p2.isitout.utils.ApplicationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
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

    public void sendEmails(NotificationLog.NotificationStatus notificationStatus) {
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
            User user = users.get(notificationLog.getUserId());
            if (user == null) {
                return;
            }
            ZeptoMailRequest request = createMailRequest(user, notificationLog, emailTemplateMap, showReleaseStateMap);
            ZeptoMailResponse response = zeptoMailEmailClient.sendMail(request);
            notificationLog.setLastAttemptAt(Instant.now());
            if(response != null && response.data() != null && !response.data().isEmpty() &&
                    ApplicationConstants.ZEPTO_SUCCESS_CODE.equals(response.data().getFirst().code())){
                log.info("Email sent successfully {}", notificationLog.getIdempotencyKey());
                notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);

            }else{
                log.error("Email sent failed: {}", notificationLog.getIdempotencyKey());
                notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
                notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
                notificationLog.setFailedAt(Instant.now());
            }
        });
        notificationLogRepository.saveAll(notificationLogList);
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
        Map<String, String> emailParams = getEmailParams(notificationLog, user.getName(), emailTemplateMap, showReleaseStateMap);
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
                .replace(ApplicationConstants.EPH_SEASON, String.valueOf(notificationLog.getSeasonNumber()))
                .replace(ApplicationConstants.EPH_EPISODE, String.valueOf(notificationLog.getEpisodeNumber()))
                .replace(ApplicationConstants.EPH_SHOW_NAME, showReleaseState.getShowName())
                .replace(ApplicationConstants.EPH_AIR_DATE, String.valueOf(showReleaseState.getNextEpisodeAirDate()));
        emailParams.put("body", emailBody);
        return emailParams;
    }
}