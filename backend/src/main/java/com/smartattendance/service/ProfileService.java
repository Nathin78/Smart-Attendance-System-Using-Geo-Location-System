package com.smartattendance.service;

import com.smartattendance.dto.AuthResponse;
import com.smartattendance.dto.UpdateProfileRequest;
import com.smartattendance.dto.UserResponse;
import com.smartattendance.entity.User;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.security.JwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncoder legacyPasswordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Qualifier("legacyPasswordEncoder") PasswordEncoder legacyPasswordEncoder,
                          UserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public UserResponse getCurrentProfile(String email) {
        User user = findUser(email);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = findUser(currentEmail);

        String updatedName = normalizeName(request.getName());
        String updatedEmail = normalizeEmail(request.getEmail());
        boolean hasPasswordChange = request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty();

        if (updatedEmail != null && !updatedEmail.equals(user.getEmail()) && userRepository.existsByEmail(updatedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (hasPasswordChange) {
            validateCurrentPassword(user, request.getCurrentPassword());
            String newPassword = request.getNewPassword().trim();
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setName(updatedName);
        user.setEmail(updatedEmail);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(
                userDetails,
                Map.of("role", user.getRole().name(), "name", user.getName())
        );
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Current password is required to change password");
        }

        String rawPassword = currentPassword.trim();
        String storedPassword = user.getPassword();
        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return;
        }
        if (legacyPasswordEncoder.matches(rawPassword, storedPassword) || rawPassword.equals(storedPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            return;
        }
        throw new IllegalArgumentException("Current password is incorrect");
    }

    private String normalizeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        return value.trim().toLowerCase();
    }
}
