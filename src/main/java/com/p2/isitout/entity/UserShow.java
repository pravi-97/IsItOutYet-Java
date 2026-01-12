package com.p2.isitout.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.p2.isitout.entity.id.UserShowId;

@Entity
@Table(name = "user_shows")
@IdClass(UserShowId.class)
@Getter
@Setter
public class UserShow {

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
