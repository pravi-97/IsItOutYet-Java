package com.p2.isitout.scheduler;

import com.p2.isitout.service.EmailNotificationSenderService;
import com.p2.isitout.service.EpisodeNotificationEnqueuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEnqueueScheduler {

    private final EpisodeNotificationEnqueuerService episodeNotificationEnqueuer;

    @Scheduled(cron = "${jobs.notification-enqueue.cron}")
    public void enqueueNotifications() {
        episodeNotificationEnqueuer.enqueue();
    }
}
