package com.p2.isitout.service;

import org.springframework.stereotype.Service;

@Service
public class ShowReleaseDetectionService {

    public void detectAndProcessNewReleases() {
        // fetch from TVMaze
        // compare with DB
        // identify new episodes/seasons
        // persist new data
        // emit domain events / call notifier
    }
}
