package com.p2.isitout.repository;

import com.p2.isitout.entity.ShowReleaseState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowReleaseStateRepository extends JpaRepository<ShowReleaseState, Integer> {
}
