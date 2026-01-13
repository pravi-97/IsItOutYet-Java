package com.p2.isitout.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ShowDto {

    private UUID showId;

    private String showName;

    private String imdbId;
    private String tmdbId;
    private String tvmazeId;

    private String status;

    private Integer lastSeasonNumber;
    private LocalDate nextSeasonAirDate;
    private Instant nextEpisodeAirDate;

    private Instant lastFetchedAt;
    private Instant createdAt;
}
