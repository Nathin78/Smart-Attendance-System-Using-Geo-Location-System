package com.smartattendance.repository;

import com.smartattendance.entity.ShiftAssignment;
import com.smartattendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {
    Optional<ShiftAssignment> findTopByUserAndActiveTrueOrderByUpdatedAtDesc(User user);

    List<ShiftAssignment> findAllByOrderByUpdatedAtDesc();
}
