package com.smartattendance.service;

import com.smartattendance.dto.AuthResponse;
import com.smartattendance.dto.UpdateProfileRequest;
import com.smartattendance.dto.UserResponse;
import com.smartattendance.entity.User;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncoder legacyPasswordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final Path avatarDirectory;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Qualifier("legacyPasswordEncoder") PasswordEncoder legacyPasswordEncoder,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          AuditLogService auditLogService,
                          @Value("${app.upload.avatar-dir:backend/uploads/avatars}") String avatarDir) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.avatarDirectory = Path.of(avatarDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.avatarDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare avatar directory", ex);
        }
    }

    public UserResponse getCurrentProfile(String email) {
        User user = findUser(email);
        UserResponse response = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
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
        auditLogService.record(currentEmail, "PROFILE_UPDATED", "USER", user.getId(), "Profile updated");

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(
                userDetails,
                Map.of("role", user.getRole().name(), "name", user.getName())
        );
        AuthResponse response = new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }

    public AuthResponse updateAvatar(String currentEmail, MultipartFile file) {
        User user = findUser(currentEmail);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "avatar.png" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        String fileName = "avatar-" + user.getId() + "-" + UUID.randomUUID() + extension;
        Path target = avatarDirectory.resolve(fileName).normalize();
        if (!target.startsWith(avatarDirectory)) {
            throw new IllegalArgumentException("Invalid avatar path");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store avatar image", ex);
        }

        user.setAvatarUrl("/uploads/avatars/" + fileName);
        userRepository.save(user);
        auditLogService.record(currentEmail, "AVATAR_UPDATED", "USER", user.getId(), "Profile photo updated");

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(
                userDetails,
                Map.of("role", user.getRole().name(), "name", user.getName())
        );
        AuthResponse response = new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
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

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return ".png";
        }
        String extension = filename.substring(idx);
        return extension.length() > 10 ? ".png" : extension.toLowerCase();
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
