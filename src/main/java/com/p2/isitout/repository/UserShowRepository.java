package com.p2.isitout.repository;

import com.p2.isitout.entity.UserShow;
import com.p2.isitout.entity.id.UserShowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserShowRepository extends JpaRepository<UserShow, UserShowId> {
    List<UserShow> findByShow_ShowIdAndNotifyNewSeasonTrue(UUID showId);

    List<UserShow> findByShow_ShowId(UUID showId);

    List<UserShow> findByShow_ShowIdAndIsTrackingTrueAndNotifyNewEpisodeTrue(UUID showId);

    @Query(value = "select tmdb_id from shows s join user_shows us on s.show_id = us.show_id where us.user_id = :userId", nativeQuery = true)
    List<String> findShowsByUserIds(UUID userId);
}
