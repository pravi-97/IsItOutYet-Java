package com.p2.isitout.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shows")
@Getter
@Setter
public class ShowDto {

    @Id
    private UUID showId;

    private String showName;

    private String imdbId;
    private Long tmdbId;
    private Long tvmazeId;

    private String status;

    private Integer lastSeasonNumber;
    private LocalDate nextSeasonAirDate;
    private Instant nextEpisodeAirDate;

    private Instant lastFetchedAt;
    private Instant createdAt;
}
