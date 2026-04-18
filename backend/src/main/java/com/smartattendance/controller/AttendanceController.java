package com.smartattendance.controller;

import com.smartattendance.dto.AttendanceResponse;
import com.smartattendance.dto.AttendanceSummaryResponse;
import com.smartattendance.dto.MarkAttendanceRequest;
import com.smartattendance.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/mark")
    public ResponseEntity<AttendanceResponse> markAttendance(@Valid @RequestBody MarkAttendanceRequest request,
                                                             Authentication authentication) {
        AttendanceResponse response = attendanceService.markAttendance(authentication.getName(), request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<List<AttendanceResponse>> getUserAttendance(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getUserAttendance(authentication.getName()));
    }

    @GetMapping("/summary")
    public ResponseEntity<AttendanceSummaryResponse> getUserSummary(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getUserSummary(authentication.getName()));
    }
}
