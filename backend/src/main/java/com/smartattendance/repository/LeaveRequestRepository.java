package com.smartattendance.repository;

import com.smartattendance.entity.LeaveRequest;
import com.smartattendance.entity.LeaveRequestStatus;
import com.smartattendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserOrderByCreatedAtDesc(User user);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequestStatus status);

    List<LeaveRequest> findByUserAndStatusOrderByCreatedAtDesc(User user, LeaveRequestStatus status);

    List<LeaveRequest> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LeaveRequestStatus status,
            LocalDate date1,
            LocalDate date2
    );
}
