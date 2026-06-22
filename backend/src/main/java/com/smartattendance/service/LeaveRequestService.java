package com.smartattendance.service;

import com.smartattendance.dto.LeaveRequestCreateRequest;
import com.smartattendance.dto.LeaveRequestResponse;
import com.smartattendance.dto.LeaveRequestReviewRequest;
import com.smartattendance.entity.LeaveRequest;
import com.smartattendance.entity.LeaveRequestStatus;
import com.smartattendance.entity.User;
import com.smartattendance.repository.LeaveRequestRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public LeaveRequestResponse createLeaveRequest(String email, LeaveRequestCreateRequest request) {
        validateRequest(request);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ensureNoOverlap(user, request.getStartDate(), request.getEndDate());

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(user);
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason().trim());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequestResponse response = mapResponse(leaveRequestRepository.save(leaveRequest));
        auditLogService.record(email, "LEAVE_REQUEST_CREATED", "LEAVE_REQUEST", response.getId(),
                "Requested leave from " + request.getStartDate() + " to " + request.getEndDate());
        notificationService.notifyUser(
                email,
                "Leave request submitted",
                "Your leave request from " + request.getStartDate() + " to " + request.getEndDate() + " is pending review.",
                "LEAVE",
                "/user-dashboard.html"
        );
        return response;
    }

    public List<LeaveRequestResponse> getMyRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return leaveRequestRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    public List<LeaveRequestResponse> getAllRequests() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    public List<LeaveRequestResponse> getPendingRequests() {
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveRequestStatus.PENDING)
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    public LeaveRequestResponse reviewLeaveRequest(String actorEmail, Long id, LeaveRequestReviewRequest request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        LeaveRequestStatus status = LeaveRequestStatus.valueOf(request.getStatus().trim().toUpperCase());
        leaveRequest.setStatus(status);
        leaveRequest.setAdminComment(sanitizeComment(request.getAdminComment()));
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequestResponse response = mapResponse(leaveRequestRepository.save(leaveRequest));
        auditLogService.record(actorEmail, "LEAVE_REQUEST_REVIEWED", "LEAVE_REQUEST", leaveRequest.getId(),
                "Leave request set to " + status.name());
        notificationService.notifyUser(
                leaveRequest.getUser().getEmail(),
                "Leave request " + status.name().toLowerCase(),
                "Your leave request from " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate()
                        + " was " + status.name().toLowerCase() + ".",
                "LEAVE",
                "/user-dashboard.html"
        );
        return response;
    }

    public boolean hasApprovedLeave(User user, LocalDate date) {
        return leaveRequestRepository.findByUserAndStatusOrderByCreatedAtDesc(user, LeaveRequestStatus.APPROVED)
                .stream()
                .anyMatch(item -> !date.isBefore(item.getStartDate()) && !date.isAfter(item.getEndDate()));
    }

    public long countApprovedLeaveForDate(LocalDate date) {
        return leaveRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        LeaveRequestStatus.APPROVED,
                        date,
                        date
                )
                .size();
    }

    private void validateRequest(LeaveRequestCreateRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            throw new IllegalArgumentException("Reason must be at least 10 characters");
        }
    }

    private void ensureNoOverlap(User user, LocalDate startDate, LocalDate endDate) {
        boolean overlaps = leaveRequestRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .anyMatch(item ->
                        !endDate.isBefore(item.getStartDate())
                                && !startDate.isAfter(item.getEndDate())
                                && item.getStatus() != LeaveRequestStatus.REJECTED
                );

        if (overlaps) {
            throw new IllegalStateException("A leave request already exists for this date range");
        }
    }

    private String sanitizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LeaveRequestResponse mapResponse(LeaveRequest leaveRequest) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        response.setId(leaveRequest.getId());
        response.setUserName(leaveRequest.getUser().getName());
        response.setUserEmail(leaveRequest.getUser().getEmail());
        response.setStartDate(leaveRequest.getStartDate());
        response.setEndDate(leaveRequest.getEndDate());
        response.setReason(leaveRequest.getReason());
        response.setStatus(leaveRequest.getStatus().name());
        response.setAdminComment(leaveRequest.getAdminComment());
        response.setCreatedAt(leaveRequest.getCreatedAt());
        response.setUpdatedAt(leaveRequest.getUpdatedAt());
        return response;
    }
}
