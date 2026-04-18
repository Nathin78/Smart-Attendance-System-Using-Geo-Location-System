package com.smartattendance.dto;

import java.time.LocalDate;
import java.util.List;

public class ReportResponse {

    private LocalDate date;
    private long totalUsers;
    private long totalMarked;
    private long presentCount;
    private long lateCount;
    private long onLeaveCount;
    private long absentCount;
    private List<AttendanceResponse> records;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalMarked() {
        return totalMarked;
    }

    public void setTotalMarked(long totalMarked) {
        this.totalMarked = totalMarked;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getLateCount() {
        return lateCount;
    }

    public void setLateCount(long lateCount) {
        this.lateCount = lateCount;
    }

    public long getOnLeaveCount() {
        return onLeaveCount;
    }

    public void setOnLeaveCount(long onLeaveCount) {
        this.onLeaveCount = onLeaveCount;
    }

    public long getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(long absentCount) {
        this.absentCount = absentCount;
    }

    public List<AttendanceResponse> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceResponse> records) {
        this.records = records;
    }
}
