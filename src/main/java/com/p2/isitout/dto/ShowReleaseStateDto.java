package com.p2.isitout.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ShowReleaseStateDto {

    private UUID showId;
    private Integer lastSeasonNumber;
    private Integer lastEpisodeNumber;

    private Integer nextSeasonNumber;
    private Integer nextEpisodeNumber;

    private LocalDate nextSeasonAirDate;
    private Instant nextEpisodeAirDate;

    private Integer lastNotifiedSeasonNumber;
    private Integer lastNotifiedEpisodeNumber;

    private Instant lastFetchedAt;
    private Instant lastNotifiedAt;
}