package com.p2.isitout.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "shows")
@Getter
@Setter
public class Show {

    @Id
    private UUID showId;

    private String showName;

    private String imdbId;
    private String tmdbId;
    private Integer tvmazeId;

    private String status;

    private Integer lastSeasonNumber;
    private Integer lastEpisodeNumber;

    private Integer nextSeasonNumber;
    private Integer nextEpisodeNumber;

    private LocalDate nextSeasonAirDate;
    private Instant nextEpisodeAirDate;

    private Instant lastFetchedAt;

    @CreationTimestamp
    private Instant createdAt;
}
