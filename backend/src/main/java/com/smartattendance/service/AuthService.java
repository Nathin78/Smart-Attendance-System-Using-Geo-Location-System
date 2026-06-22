package com.smartattendance.service;

import com.smartattendance.dto.AuthResponse;
import com.smartattendance.dto.LoginRequest;
import com.smartattendance.dto.RegisterRequest;
import com.smartattendance.entity.Role;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncoder legacyPasswordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
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

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(
                userDetails,
                Map.of("role", user.getRole().name(), "name", user.getName())
        );
        AuthResponse response = new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String rawPassword = request.getPassword();
        String storedPassword = user.getPassword();

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            // Password already stored in the new alphanumeric format.
        } else if (legacyPasswordEncoder.matches(rawPassword, storedPassword) || rawPassword.equals(storedPassword)) {
            // Upgrade legacy passwords to the new alphanumeric hash format.
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(
                userDetails,
                Map.of("role", user.getRole().name(), "name", user.getName())
        );
        AuthResponse response = new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }
}
