package com.smartattendance.service;

import com.smartattendance.dto.AttendanceResponse;
import com.smartattendance.dto.ReportResponse;
import com.smartattendance.dto.UserResponse;
import com.smartattendance.entity.Attendance;
import com.smartattendance.entity.AttendanceStatus;
import com.smartattendance.entity.User;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final LeaveRequestService leaveRequestService;

    public AdminService(UserRepository userRepository,
                        AttendanceRepository attendanceRepository,
                        AttendanceService attendanceService,
                        LeaveRequestService leaveRequestService) {
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.leaveRequestService = leaveRequestService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name()
                ))
                .toList();
    }

    public ReportResponse getReport(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        long totalUsers = userRepository.count();
        long totalMarked = attendanceService.countByDate(targetDate);
        long presentCount = attendanceService.countByDateAndStatus(targetDate, AttendanceStatus.PRESENT);
        long lateCount = attendanceService.countByDateAndStatus(targetDate, AttendanceStatus.LATE);
        long onLeaveCount = leaveRequestService.countApprovedLeaveForDate(targetDate);
        long absentCount = Math.max(totalUsers - totalMarked - onLeaveCount, 0);
        List<AttendanceResponse> records = attendanceService.getAttendanceByDate(targetDate);

        ReportResponse response = new ReportResponse();
        response.setDate(targetDate);
        response.setTotalUsers(totalUsers);
        response.setTotalMarked(totalMarked);
        response.setPresentCount(presentCount);
        response.setLateCount(lateCount);
        response.setOnLeaveCount(onLeaveCount);
        response.setAbsentCount(absentCount);
        response.setRecords(records);
        return response;
    }

    public byte[] generateAttendanceReportExcel(String type, LocalDate referenceDate) {
        LocalDate baseDate = referenceDate != null ? referenceDate : LocalDate.now();
        PeriodWindow period = resolvePeriod(type, baseDate);

        List<User> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<Attendance> attendanceRecords = attendanceRepository.findByDateBetweenOrderByDateAscTimeAsc(
                period.startDate(),
                period.endDate()
        );

        Map<String, Attendance> latestByUserDate = new HashMap<>();
        for (Attendance record : attendanceRecords) {
            String key = buildKey(record.getUser().getId(), record.getDate());
            Attendance existing = latestByUserDate.get(key);
            if (existing == null || record.getTime().isAfter(existing.getTime())) {
                latestByUserDate.put(key, record);
            }
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet summarySheet = workbook.createSheet("Summary");
            Sheet attendanceSheet = workbook.createSheet("Attendance");

            CellStyle headerStyle = createHeaderStyle(workbook);

            int expectedRows = users.size() * period.daysCount();
            int presentCount = 0;
            int lateCount = 0;
            int absentCount = 0;

            Row attendanceHeader = attendanceSheet.createRow(0);
            String[] headers = {"Date", "User ID", "Name", "Email", "Role", "Status", "Time", "Latitude", "Longitude", "Distance (m)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = attendanceHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            LocalDate current = period.startDate();
            while (!current.isAfter(period.endDate())) {
                for (User user : users) {
                    String key = buildKey(user.getId(), current);
                    Attendance record = latestByUserDate.get(key);

                    String status;
                    String time = "-";
                    String latitude = "-";
                    String longitude = "-";
                    String distance = "-";

                    if (record == null) {
                        status = "ABSENT";
                        absentCount++;
                    } else {
                        status = record.getStatus().name();
                        LocalTime markedTime = record.getTime();
                        time = markedTime != null ? markedTime.toString() : "-";
                        latitude = record.getLatitude() != null ? record.getLatitude().toString() : "-";
                        longitude = record.getLongitude() != null ? record.getLongitude().toString() : "-";
                        distance = record.getDistanceMeters() != null ? record.getDistanceMeters().toString() : "-";
                        if (record.getStatus() == AttendanceStatus.PRESENT) {
                            presentCount++;
                        } else if (record.getStatus() == AttendanceStatus.LATE) {
                            lateCount++;
                        }
                    }

                    Row row = attendanceSheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(current.toString());
                    row.createCell(1).setCellValue(user.getId() != null ? user.getId() : -1);
                    row.createCell(2).setCellValue(user.getName() != null ? user.getName() : "-");
                    row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "-");
                    row.createCell(4).setCellValue(user.getRole() != null ? user.getRole().name() : "-");
                    row.createCell(5).setCellValue(status);
                    row.createCell(6).setCellValue(time);
                    row.createCell(7).setCellValue(latitude);
                    row.createCell(8).setCellValue(longitude);
                    row.createCell(9).setCellValue(distance);
                }
                current = current.plusDays(1);
            }

            addSummaryRow(summarySheet, 0, "Report Type", period.label(), headerStyle);
            addSummaryRow(summarySheet, 1, "From", period.startDate().toString(), headerStyle);
            addSummaryRow(summarySheet, 2, "To", period.endDate().toString(), headerStyle);
            addSummaryRow(summarySheet, 3, "Total Users", String.valueOf(users.size()), headerStyle);
            addSummaryRow(summarySheet, 4, "Days In Period", String.valueOf(period.daysCount()), headerStyle);
            addSummaryRow(summarySheet, 5, "Expected Entries", String.valueOf(expectedRows), headerStyle);
            addSummaryRow(summarySheet, 6, "Present", String.valueOf(presentCount), headerStyle);
            addSummaryRow(summarySheet, 7, "Late", String.valueOf(lateCount), headerStyle);
            addSummaryRow(summarySheet, 8, "Absent", String.valueOf(absentCount), headerStyle);

            for (int i = 0; i < headers.length; i++) {
                attendanceSheet.autoSizeColumn(i);
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate Excel report");
        }
    }

    private PeriodWindow resolvePeriod(String type, LocalDate baseDate) {
        String normalized = type == null ? "daily" : type.trim().toLowerCase();
        return switch (normalized) {
            case "weekly" -> new PeriodWindow(
                    baseDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                    baseDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)),
                    "Weekly"
            );
            case "monthly" -> new PeriodWindow(
                    baseDate.with(TemporalAdjusters.firstDayOfMonth()),
                    baseDate.with(TemporalAdjusters.lastDayOfMonth()),
                    "Monthly"
            );
            case "daily" -> new PeriodWindow(baseDate, baseDate, "Daily");
            default -> throw new IllegalArgumentException("Invalid report type. Use daily, weekly, or monthly");
        };
    }

    private String buildKey(Long userId, LocalDate date) {
        return userId + "|" + date;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void addSummaryRow(Sheet sheet, int rowIndex, String key, String value, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value);
    }

    private record PeriodWindow(LocalDate startDate, LocalDate endDate, String label) {
        int daysCount() {
            return (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
        }
    }
}
