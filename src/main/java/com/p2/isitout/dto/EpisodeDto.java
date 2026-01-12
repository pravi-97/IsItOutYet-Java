package com.p2.isitout.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "episodes",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "showId", "seasonNumber", "episodeNumber"
        }))
@Getter
@Setter
public class EpisodeDto {

    @Id
    private UUID episodeId;

    private UUID showId;

    private int seasonNumber;
    private int episodeNumber;

    private String episodeName;
    private Instant airDate;
    private String status;
}
