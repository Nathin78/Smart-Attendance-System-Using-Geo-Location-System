package com.smartattendance.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AttendanceSummaryResponse {

    private String userName;
    private String userEmail;
    private long totalRecords;
    private long presentCount;
    private long lateCount;
    private long currentStreak;
    private long bestStreak;
    private long thisMonthRecords;
    private long thisMonthPresent;
    private long thisMonthLate;
    private double monthlyCompletionRate;
    private LocalDateTime generatedAt;
    private AttendanceResponse todayRecord;
    private AttendanceResponse lastRecord;
    private List<AttendanceResponse> recentRecords;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
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

    public long getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(long currentStreak) {
        this.currentStreak = currentStreak;
    }

    public long getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(long bestStreak) {
        this.bestStreak = bestStreak;
    }

    public long getThisMonthRecords() {
        return thisMonthRecords;
    }

    public void setThisMonthRecords(long thisMonthRecords) {
        this.thisMonthRecords = thisMonthRecords;
    }

    public long getThisMonthPresent() {
        return thisMonthPresent;
    }

    public void setThisMonthPresent(long thisMonthPresent) {
        this.thisMonthPresent = thisMonthPresent;
    }

    public long getThisMonthLate() {
        return thisMonthLate;
    }

    public void setThisMonthLate(long thisMonthLate) {
        this.thisMonthLate = thisMonthLate;
    }

    public double getMonthlyCompletionRate() {
        return monthlyCompletionRate;
    }

    public void setMonthlyCompletionRate(double monthlyCompletionRate) {
        this.monthlyCompletionRate = monthlyCompletionRate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public AttendanceResponse getTodayRecord() {
        return todayRecord;
    }

    public void setTodayRecord(AttendanceResponse todayRecord) {
        this.todayRecord = todayRecord;
    }

    public AttendanceResponse getLastRecord() {
        return lastRecord;
    }

    public void setLastRecord(AttendanceResponse lastRecord) {
        this.lastRecord = lastRecord;
    }

    public List<AttendanceResponse> getRecentRecords() {
        return recentRecords;
    }

    public void setRecentRecords(List<AttendanceResponse> recentRecords) {
        this.recentRecords = recentRecords;
    }
}
