package com.p2.isitout.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
public class ShowReleaseState {

    @Id
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
}
