package com.smartattendance.repository;

import com.smartattendance.entity.Attendance;
import com.smartattendance.entity.AttendanceStatus;
import com.smartattendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByUserAndDate(User user, LocalDate date);

    List<Attendance> findByUserOrderByDateDescTimeDesc(User user);

    List<Attendance> findByDateOrderByTimeDesc(LocalDate date);

    List<Attendance> findByDateBetweenOrderByDateAscTimeAsc(LocalDate startDate, LocalDate endDate);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    long countByDate(LocalDate date);
}
