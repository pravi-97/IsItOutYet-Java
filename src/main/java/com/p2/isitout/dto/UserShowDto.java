package com.p2.isitout.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_shows")
@IdClass(UserShowIdDto.class)
@Getter
@Setter
public class UserShowDto {

    @Id
    private java.util.UUID userId;

    @Id
    private java.util.UUID showId;

    private boolean notifyNewEpisode;
    private boolean notifyNewSeason;
    private boolean notifySeasonConfirmed;

    private boolean isTracking;
    private Instant lastNotifiedEpisodeAirDate;

    private Instant addedAt;
}
