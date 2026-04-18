package com.smartattendance.config;

import com.smartattendance.entity.Geofence;
import com.smartattendance.entity.Role;
import com.smartattendance.entity.User;
import com.smartattendance.repository.GeofenceRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final double BOOTSTRAP_LATITUDE = 12.9716;
    private static final double BOOTSTRAP_LONGITUDE = 77.5946;
    private static final double OLD_BOOTSTRAP_RADIUS = 300.0;

    private final UserRepository userRepository;
    private final GeofenceRepository geofenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final double defaultLatitude;
    private final double defaultLongitude;
    private final double defaultRadius;

    public DataInitializer(UserRepository userRepository,
                           GeofenceRepository geofenceRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.name}") String adminName,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword,
                           @Value("${app.geofence.default-latitude}") double defaultLatitude,
                           @Value("${app.geofence.default-longitude}") double defaultLongitude,
                           @Value("${app.geofence.default-radius}") double defaultRadius) {
        this.userRepository = userRepository;
        this.geofenceRepository = geofenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
        this.defaultRadius = defaultRadius;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setName(adminName.trim());
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (!geofenceRepository.findTopByOrderByIdAsc().isPresent()) {
            Geofence geofence = new Geofence();
            geofence.setLatitude(defaultLatitude);
            geofence.setLongitude(defaultLongitude);
            geofence.setRadius(defaultRadius);
            geofenceRepository.save(geofence);
            return;
        }

        Geofence geofence = geofenceRepository.findTopByOrderByIdAsc().orElse(null);
        if (geofence != null
                && Double.compare(geofence.getLatitude(), BOOTSTRAP_LATITUDE) == 0
                && Double.compare(geofence.getLongitude(), BOOTSTRAP_LONGITUDE) == 0
                && (geofence.getRadius() == null || geofence.getRadius() <= OLD_BOOTSTRAP_RADIUS)) {
            geofence.setRadius(defaultRadius);
            geofenceRepository.save(geofence);
        }
    }
}
