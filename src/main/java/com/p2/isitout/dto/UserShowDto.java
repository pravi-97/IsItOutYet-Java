package com.p2.isitout.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserShowDto {

    private UUID userId;
    private UUID showId;

    private boolean notifyNewEpisode;
    private boolean notifyNewSeason;
    private boolean notifySeasonConfirmed;

    private boolean isTracking;

    private Instant addedAt;
}
