package com.ezdo.repository;

import com.ezdo.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StreakRepository extends JpaRepository<Streak, UUID> {
}
