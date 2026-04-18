package com.smartattendance.controller;

import com.smartattendance.dto.AuthResponse;
import com.smartattendance.dto.UpdateProfileRequest;
import com.smartattendance.dto.UserResponse;
import com.smartattendance.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getCurrentProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateCurrentProfile(@RequestBody UpdateProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(profileService.updateProfile(authentication.getName(), request));
    }
}
