package com.p2.isitout.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
public class ShowReleaseState {

    @Id
    @Column(name = "show_id")
    private UUID showId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id")
    private Show show;

    private String showName;

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

    private Integer currentSeasonNumber;

    private Boolean nextEpisodeReminderQueued;

}