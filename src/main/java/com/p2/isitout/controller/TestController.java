package com.p2.isitout.controller;

import com.p2.isitout.entity.NotificationLog;
import com.p2.isitout.service.EmailNotificationSenderService;
import com.p2.isitout.service.EpisodeNotificationEnqueuerService;
import com.p2.isitout.service.ShowReleaseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final EmailNotificationSenderService  emailNotificationSenderService;
    private final ShowReleaseDetectionService showReleaseDetectionService;
    private final EpisodeNotificationEnqueuerService episodeNotificationEnqueuerService;

    @PostMapping("/send/mail")
    public Object sendEMail(){
        // showReleaseDetectionService.detectAndProcessNewReleases();
        // episodeNotificationEnqueuerService.enqueue();
        // emailNotificationSenderService.sendEmails(NotificationLog.NotificationStatus.PENDING);
        return "Success";
    }

}
