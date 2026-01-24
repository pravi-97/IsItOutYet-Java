package com.p2.isitout.repository;

import com.p2.isitout.entity.ShowReleaseState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowReleaseStateRepository extends JpaRepository<ShowReleaseState, UUID> {

    List<ShowReleaseState> findAllByShow_Status(String running);

    // "Find shows airing in the given time window"
    @Query("SELECT s FROM ShowReleaseState s WHERE s.nextEpisodeAirDate BETWEEN :start AND :end")
    List<ShowReleaseState> findShowsAiringBetween(@Param("start") Instant start, @Param("end") Instant end);
}
