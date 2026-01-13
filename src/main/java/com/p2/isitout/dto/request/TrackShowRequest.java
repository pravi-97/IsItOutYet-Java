package com.p2.isitout.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackShowRequest {

    private String tmdbId;
    private String tvmazeId;

    private boolean notifyNewEpisode;
    private boolean notifyNewSeason;
    private boolean notifySeasonConfirmed;

    private boolean tracking = true;
}