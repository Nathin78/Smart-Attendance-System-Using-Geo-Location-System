package com.smartattendance.service;

import com.smartattendance.dto.GeofenceRequest;
import com.smartattendance.dto.GeofenceResponse;
import com.smartattendance.entity.Geofence;
import com.smartattendance.repository.GeofenceRepository;
import org.springframework.stereotype.Service;

@Service
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;

    public GeofenceService(GeofenceRepository geofenceRepository) {
        this.geofenceRepository = geofenceRepository;
    }

    public Geofence getGeofenceEntity() {
        return geofenceRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Geofence is not configured"));
    }

    public GeofenceResponse getCurrentGeofence() {
        Geofence geofence = getGeofenceEntity();
        return mapToResponse(geofence);
    }

    public GeofenceResponse updateGeofence(GeofenceRequest request) {
        Geofence geofence = geofenceRepository.findTopByOrderByIdAsc().orElseGet(Geofence::new);
        geofence.setLatitude(request.getLatitude());
        geofence.setLongitude(request.getLongitude());
        geofence.setRadius(request.getRadius());
        geofenceRepository.save(geofence);
        return mapToResponse(geofence);
    }

    private GeofenceResponse mapToResponse(Geofence geofence) {
        return new GeofenceResponse(
                geofence.getId(),
                geofence.getLatitude(),
                geofence.getLongitude(),
                geofence.getRadius()
        );
    }
}
