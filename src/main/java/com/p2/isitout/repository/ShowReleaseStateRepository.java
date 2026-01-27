package com.p2.isitout.repository;

import com.p2.isitout.entity.ShowReleaseState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ShowReleaseStateRepository extends JpaRepository<ShowReleaseState, UUID> {

    List<ShowReleaseState> findAllByShow_Status(String running);

    @Query("SELECT s FROM ShowReleaseState s WHERE s.nextEpisodeAirDate IS NOT NULL")
    List<ShowReleaseState> findStatesWithNextEpisode();
}
