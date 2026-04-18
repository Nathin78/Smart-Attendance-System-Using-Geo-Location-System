package com.smartattendance.service;

import com.smartattendance.dto.AttendanceResponse;
import com.smartattendance.dto.AttendanceSummaryResponse;
import com.smartattendance.dto.MarkAttendanceRequest;
import com.smartattendance.entity.Attendance;
import com.smartattendance.entity.AttendanceStatus;
import com.smartattendance.entity.Geofence;
import com.smartattendance.entity.User;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AttendanceService {

    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final GeofenceService geofenceService;
    private final LeaveRequestService leaveRequestService;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final LocalTime lateAfter;
    private final boolean allowWeekend;
    private final double maxAccuracyMeters;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             UserRepository userRepository,
                             GeofenceService geofenceService,
                             LeaveRequestService leaveRequestService,
                             @Value("${app.attendance.start-time}") String startTime,
                             @Value("${app.attendance.end-time}") String endTime,
                             @Value("${app.attendance.late-after}") String lateAfter,
                             @Value("${app.attendance.allow-weekend}") boolean allowWeekend,
                             @Value("${app.attendance.max-accuracy-meters}") double maxAccuracyMeters) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.geofenceService = geofenceService;
        this.leaveRequestService = leaveRequestService;
        this.startTime = LocalTime.parse(startTime);
        this.endTime = LocalTime.parse(endTime);
        this.lateAfter = LocalTime.parse(lateAfter);
        this.allowWeekend = allowWeekend;
        this.maxAccuracyMeters = maxAccuracyMeters;
    }

    public AttendanceResponse markAttendance(String email, MarkAttendanceRequest request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
        validateAccuracy(request.getAccuracyMeters());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (!allowWeekend && (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)) {
            throw new IllegalStateException("Attendance marking is disabled on weekends");
        }

        if (leaveRequestService.hasApprovedLeave(user, today)) {
            throw new IllegalStateException("Approved leave exists for today. Attendance is not required.");
        }

        if (attendanceRepository.existsByUserAndDate(user, today)) {
            throw new IllegalStateException("Attendance already marked for today");
        }

        Geofence geofence = geofenceService.getGeofenceEntity();
        double distanceMeters = calculateDistanceMeters(
                request.getLatitude(),
                request.getLongitude(),
                geofence.getLatitude(),
                geofence.getLongitude()
        );

        if (distanceMeters > geofence.getRadius()) {
            AttendanceResponse response = new AttendanceResponse();
            response.setSuccess(false);
            response.setMessage("Invalid location. You are outside the allowed geofence.");
            response.setDistanceMeters(round(distanceMeters));
            return response;
        }

        LocalTime currentTime = LocalTime.now();
        if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
            throw new IllegalStateException(
                    "Attendance can be marked only between "
                            + startTime.format(TIME_FORMATTER)
                            + " and "
                            + endTime.format(TIME_FORMATTER)
            );
        }

        AttendanceStatus status = currentTime.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setDate(today);
        attendance.setTime(currentTime);
        attendance.setLatitude(request.getLatitude());
        attendance.setLongitude(request.getLongitude());
        attendance.setStatus(status);
        attendance.setCreatedAt(LocalDateTime.now());
        attendance.setDistanceMeters(round(distanceMeters));
        attendance.setAccuracyMeters(request.getAccuracyMeters());
        attendance.setNote(sanitizeNote(request.getNote()));
        try {
            attendanceRepository.save(attendance);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Attendance already marked for today");
        }

        AttendanceResponse response = mapAttendance(attendance);
        response.setSuccess(true);
        response.setMessage("Attendance marked successfully");
        return response;
    }

    public List<AttendanceResponse> getUserAttendance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return attendanceRepository.findByUserOrderByDateDescTimeDesc(user)
                .stream()
                .map(this::mapAttendance)
                .toList();
    }

    public AttendanceSummaryResponse getUserSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Attendance> records = attendanceRepository.findByUserOrderByDateDescTimeDesc(user);
        List<AttendanceResponse> mappedRecords = records.stream()
                .map(this::mapAttendance)
                .toList();

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        long totalRecords = records.size();
        long presentCount = records.stream().filter(item -> item.getStatus() == AttendanceStatus.PRESENT).count();
        long lateCount = records.stream().filter(item -> item.getStatus() == AttendanceStatus.LATE).count();
        long thisMonthRecords = records.stream()
                .filter(item -> YearMonth.from(item.getDate()).equals(currentMonth))
                .count();
        long thisMonthPresent = records.stream()
                .filter(item -> YearMonth.from(item.getDate()).equals(currentMonth))
                .filter(item -> item.getStatus() == AttendanceStatus.PRESENT)
                .count();
        long thisMonthLate = records.stream()
                .filter(item -> YearMonth.from(item.getDate()).equals(currentMonth))
                .filter(item -> item.getStatus() == AttendanceStatus.LATE)
                .count();

        AttendanceResponse todayRecord = records.stream()
                .filter(item -> today.equals(item.getDate()))
                .findFirst()
                .map(this::mapAttendance)
                .orElse(null);

        AttendanceResponse lastRecord = records.isEmpty() ? null : mapAttendance(records.get(0));

        AttendanceSummaryResponse summary = new AttendanceSummaryResponse();
        summary.setUserName(user.getName());
        summary.setUserEmail(user.getEmail());
        summary.setTotalRecords(totalRecords);
        summary.setPresentCount(presentCount);
        summary.setLateCount(lateCount);
        summary.setCurrentStreak(calculateCurrentStreak(records));
        summary.setBestStreak(calculateBestStreak(records));
        summary.setThisMonthRecords(thisMonthRecords);
        summary.setThisMonthPresent(thisMonthPresent);
        summary.setThisMonthLate(thisMonthLate);
        summary.setMonthlyCompletionRate(calculateMonthlyCompletionRate(thisMonthRecords, today));
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setTodayRecord(todayRecord);
        summary.setLastRecord(lastRecord);
        summary.setRecentRecords(mappedRecords.stream().limit(5).toList());
        return summary;
    }

    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByTimeDesc(date)
                .stream()
                .map(this::mapAttendance)
                .toList();
    }

    public long countByDate(LocalDate date) {
        return attendanceRepository.countByDate(date);
    }

    public long countByDateAndStatus(LocalDate date, AttendanceStatus status) {
        return attendanceRepository.countByDateAndStatus(date, status);
    }

    private AttendanceResponse mapAttendance(Attendance attendance) {
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setStatus(attendance.getStatus().name());
        response.setUserName(attendance.getUser().getName());
        response.setUserEmail(attendance.getUser().getEmail());
        response.setDate(attendance.getDate());
        response.setTime(attendance.getTime());
        response.setLatitude(attendance.getLatitude());
        response.setLongitude(attendance.getLongitude());
        response.setDistanceMeters(attendance.getDistanceMeters());
        response.setAccuracyMeters(attendance.getAccuracyMeters());
        response.setNote(attendance.getNote());
        response.setMessage("Record fetched");
        return response;
    }

    private long calculateCurrentStreak(List<Attendance> records) {
        if (records.isEmpty()) {
            return 0;
        }

        List<LocalDate> uniqueDates = records.stream()
                .map(Attendance::getDate)
                .distinct()
                .toList();
        long streak = 1;

        for (int i = 1; i < uniqueDates.size(); i++) {
            LocalDate previous = uniqueDates.get(i - 1);
            LocalDate current = uniqueDates.get(i);
            if (previous.minusDays(1).equals(current)) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    private long calculateBestStreak(List<Attendance> records) {
        if (records.isEmpty()) {
            return 0;
        }

        List<LocalDate> uniqueDates = records.stream()
                .map(Attendance::getDate)
                .distinct()
                .sorted()
                .toList();

        long best = 1;
        long current = 1;
        LocalDate previous = uniqueDates.get(0);

        for (int i = 1; i < uniqueDates.size(); i++) {
            LocalDate date = uniqueDates.get(i);
            if (previous.plusDays(1).equals(date)) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
            previous = date;
        }

        return best;
    }

    private double calculateMonthlyCompletionRate(long thisMonthRecords, LocalDate today) {
        int elapsedDays = Math.max(today.getDayOfMonth(), 1);
        return round((thisMonthRecords * 100.0) / elapsedDays);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }

    private void validateAccuracy(Double accuracyMeters) {
        if (accuracyMeters == null) {
            return;
        }

        if (accuracyMeters <= 0) {
            throw new IllegalArgumentException("GPS accuracy must be greater than 0");
        }

        if (accuracyMeters > maxAccuracyMeters) {
            throw new IllegalArgumentException(
                    "GPS accuracy is too low (" + round(accuracyMeters) + " m). Move to open area and try again."
            );
        }
    }

    private String sanitizeNote(String note) {
        if (note == null) {
            return null;
        }

        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
