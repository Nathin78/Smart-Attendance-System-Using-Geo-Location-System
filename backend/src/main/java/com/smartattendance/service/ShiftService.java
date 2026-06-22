package com.smartattendance.service;

import com.smartattendance.dto.ShiftRequest;
import com.smartattendance.dto.ShiftResponse;
import com.smartattendance.entity.ShiftAssignment;
import com.smartattendance.entity.User;
import com.smartattendance.repository.ShiftAssignmentRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final UserRepository userRepository;

    public ShiftService(ShiftAssignmentRepository shiftAssignmentRepository, UserRepository userRepository) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.userRepository = userRepository;
    }

    public ShiftResponse getCurrentShift(String email) {
        User user = findUser(email);
        return shiftAssignmentRepository.findTopByUserAndActiveTrueOrderByUpdatedAtDesc(user)
                .map(this::map)
                .orElse(null);
    }

    public List<ShiftResponse> getAllShifts() {
        return shiftAssignmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::map)
                .toList();
    }

    public ShiftResponse upsertShift(Long userId, ShiftRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ShiftAssignment shift = shiftAssignmentRepository.findTopByUserAndActiveTrueOrderByUpdatedAtDesc(user)
                .orElseGet(ShiftAssignment::new);
        shift.setUser(user);
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setLateAfter(request.getLateAfter());
        shift.setActive(request.isActive());
        shift.setUpdatedAt(LocalDateTime.now());
        return map(shiftAssignmentRepository.save(shift));
    }

    public ShiftAssignment getActiveShiftEntity(String email) {
        User user = findUser(email);
        return shiftAssignmentRepository.findTopByUserAndActiveTrueOrderByUpdatedAtDesc(user).orElse(null);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private ShiftResponse map(ShiftAssignment shift) {
        ShiftResponse response = new ShiftResponse();
        response.setId(shift.getId());
        response.setUserId(shift.getUser().getId());
        response.setUserName(shift.getUser().getName());
        response.setUserEmail(shift.getUser().getEmail());
        response.setStartTime(shift.getStartTime());
        response.setEndTime(shift.getEndTime());
        response.setLateAfter(shift.getLateAfter());
        response.setActive(shift.isActive());
        response.setUpdatedAt(shift.getUpdatedAt());
        return response;
    }
}
