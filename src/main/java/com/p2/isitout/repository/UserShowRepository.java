package com.p2.isitout.repository;

import com.p2.isitout.entity.UserShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserShowRepository extends JpaRepository<UserShow, Integer> {
}
