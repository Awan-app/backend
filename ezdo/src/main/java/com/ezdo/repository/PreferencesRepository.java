package com.ezdo.repository;

import com.ezdo.entity.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreferencesRepository extends JpaRepository<Preferences, UUID> {
}
