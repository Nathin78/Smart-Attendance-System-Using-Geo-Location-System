package com.smartattendance.controller;

import com.smartattendance.dto.GeofenceRequest;
import com.smartattendance.dto.GeofenceResponse;
import com.smartattendance.dto.ReportResponse;
import com.smartattendance.dto.AuditLogResponse;
import com.smartattendance.dto.LeaveRequestResponse;
import com.smartattendance.dto.LeaveRequestReviewRequest;
import com.smartattendance.dto.RoleUpdateRequest;
import com.smartattendance.dto.ShiftRequest;
import com.smartattendance.dto.ShiftResponse;
import com.smartattendance.dto.UserResponse;
import com.smartattendance.service.AdminService;
import com.smartattendance.service.GeofenceService;
import com.smartattendance.service.LeaveRequestService;
import com.smartattendance.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final GeofenceService geofenceService;
    private final LeaveRequestService leaveRequestService;
    private final ShiftService shiftService;

    public AdminController(AdminService adminService, GeofenceService geofenceService, LeaveRequestService leaveRequestService,
                           ShiftService shiftService) {
        this.adminService = adminService;
        this.geofenceService = geofenceService;
        this.leaveRequestService = leaveRequestService;
        this.shiftService = shiftService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/reports")
    public ResponseEntity<ReportResponse> getReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getReport(date));
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReports(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        byte[] file = adminService.generateAttendanceReportExcel(type, date);
        LocalDate baseDate = date != null ? date : LocalDate.now();
        String filename = String.format("attendance-%s-%s.xlsx", type.toLowerCase(), baseDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(file);
    }

    @GetMapping("/geofence")
    public ResponseEntity<GeofenceResponse> getGeofence() {
        return ResponseEntity.ok(geofenceService.getCurrentGeofence());
    }

    @PutMapping("/geofence")
    public ResponseEntity<GeofenceResponse> updateGeofence(@Valid @RequestBody GeofenceRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(geofenceService.updateGeofence(authentication.getName(), request));
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequests(
            @RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(leaveRequestService.getAllRequests());
        }

        String normalized = status.trim().toUpperCase();
        if ("PENDING".equals(normalized)) {
            return ResponseEntity.ok(leaveRequestService.getPendingRequests());
        }
        return ResponseEntity.ok(leaveRequestService.getAllRequests());
    }

    @PutMapping("/leave-requests/{id}")
    public ResponseEntity<LeaveRequestResponse> reviewLeaveRequest(@PathVariable Long id,
                                                                   @Valid @RequestBody LeaveRequestReviewRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.reviewLeaveRequest(authentication.getName(), id, request));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody RoleUpdateRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request, authentication.getName()));
    }

    @GetMapping("/shifts")
    public ResponseEntity<List<ShiftResponse>> getShifts() {
        return ResponseEntity.ok(adminService.getAllShifts());
    }

    @PutMapping("/shifts/{userId}")
    public ResponseEntity<ShiftResponse> updateShift(@PathVariable Long userId,
                                                     @Valid @RequestBody ShiftRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(adminService.updateShift(userId, request, authentication.getName()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}
