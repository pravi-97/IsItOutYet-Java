package com.p2.isitout.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "episodes",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "showId", "seasonNumber", "episodeNumber"
        }))
@Getter
@Setter
public class Episode {

    @Id
    private UUID episodeId;

    private UUID showId;

    private int seasonNumber;
    private int episodeNumber;

    private String episodeName;
    private Instant airDate;
    private String status;
}
