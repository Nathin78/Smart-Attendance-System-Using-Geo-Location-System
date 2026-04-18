package com.smartattendance.repository;

import com.smartattendance.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    Optional<Geofence> findTopByOrderByIdAsc();
}
