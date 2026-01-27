package com.p2.isitout.scheduler;

import com.p2.isitout.service.EmailNotificationSenderService;
import com.p2.isitout.service.ShowReleaseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendEmailScheduler {

    private final EmailNotificationSenderService emailNotificationSenderService;

    @Scheduled(cron = "${jobs.email-send.cron}")
    public void sendPendingEmails() {
        emailNotificationSenderService.sendPending();
    }
}
