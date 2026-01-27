package com.p2.isitout.scheduler;

import com.p2.isitout.service.ShowReleaseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShowReleasePollingScheduler {

    private final ShowReleaseDetectionService showReleaseDetectionService;

    @Scheduled(cron = "${jobs.show-release-polling.cron}")
    public void pollForNewReleases() {
        showReleaseDetectionService.detectAndProcessNewReleases();
    }
}
