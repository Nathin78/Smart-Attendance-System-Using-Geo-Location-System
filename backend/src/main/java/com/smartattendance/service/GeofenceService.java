package com.smartattendance.service;

import com.smartattendance.dto.GeofenceRequest;
import com.smartattendance.dto.GeofenceResponse;
import com.smartattendance.entity.Geofence;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.repository.GeofenceRepository;
import org.springframework.stereotype.Service;

@Service
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public GeofenceService(GeofenceRepository geofenceRepository,
                           NotificationService notificationService,
                           AuditLogService auditLogService,
                           UserRepository userRepository) {
        this.geofenceRepository = geofenceRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    public Geofence getGeofenceEntity() {
        return geofenceRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Geofence is not configured"));
    }

    public GeofenceResponse getCurrentGeofence() {
        Geofence geofence = getGeofenceEntity();
        return mapToResponse(geofence);
    }

    public GeofenceResponse updateGeofence(String actorEmail, GeofenceRequest request) {
        Geofence geofence = geofenceRepository.findTopByOrderByIdAsc().orElseGet(Geofence::new);
        geofence.setLatitude(request.getLatitude());
        geofence.setLongitude(request.getLongitude());
        geofence.setRadius(request.getRadius());
        geofenceRepository.save(geofence);
        notificationService.notifyAllUsers(
                "Geofence updated",
                "The attendance geofence has been updated. Please review the latest center and radius before marking attendance.",
                "GEOFENCE",
                "/user-dashboard.html"
        );
        auditLogService.record(actorEmail, "GEOFENCE_UPDATED", "GEOFENCE", geofence.getId(),
                "Updated geofence to lat=" + request.getLatitude() + ", lon=" + request.getLongitude()
                        + ", radius=" + request.getRadius());
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
