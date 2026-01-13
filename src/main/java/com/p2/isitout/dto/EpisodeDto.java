package com.p2.isitout.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class EpisodeDto {

    private UUID episodeId;

    private UUID showId;

    private int seasonNumber;
    private int episodeNumber;

    private String episodeName;
    private Instant airDate;
    private String status;
}
