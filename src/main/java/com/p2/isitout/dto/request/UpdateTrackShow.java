package com.p2.isitout.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTrackShow {
    private UUID showId;
    private String name;
    private String posterPath;
    private String firstAirDate;
    private String addedAt;
    private Boolean notifyEpisodeAfterRelease;
    private Boolean notifyEpisodeDayBefore;
    private Boolean notifyEpisodeOnReleaseDay;
    private Boolean notifyNewEpisode;
    private Boolean notifyNewSeason;
    private Boolean notifySeasonConfirmed;
    private Boolean tracking;
    private UUID userId;
}