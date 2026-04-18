package com.smartattendance.controller;

import com.smartattendance.dto.LeaveRequestCreateRequest;
import com.smartattendance.dto.LeaveRequestResponse;
import com.smartattendance.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody LeaveRequestCreateRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.createLeaveRequest(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(leaveRequestService.getMyRequests(authentication.getName()));
    }
}
