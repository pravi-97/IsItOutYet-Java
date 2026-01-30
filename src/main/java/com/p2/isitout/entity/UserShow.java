package com.p2.isitout.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.p2.isitout.entity.id.UserShowId;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_shows")
@Getter
@Setter
public class UserShow {

    @EmbeddedId
    private UserShowId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("showId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    private boolean notifyNewEpisode;
    private boolean notifyEpisodeDayBefore;
    private boolean notifyEpisodeOnReleaseDay;
    private boolean notifyEpisodeAfterRelease;
    private boolean notifyNewSeason;
    private boolean notifySeasonConfirmed;

    private boolean isTracking;

    @CreationTimestamp
    private Instant addedAt;
}
