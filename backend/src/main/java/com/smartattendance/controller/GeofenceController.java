package com.smartattendance.controller;

import com.smartattendance.dto.GeofenceResponse;
import com.smartattendance.service.GeofenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geofence")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @GetMapping("/current")
    public ResponseEntity<GeofenceResponse> getCurrentGeofence() {
        return ResponseEntity.ok(geofenceService.getCurrentGeofence());
    }
}
