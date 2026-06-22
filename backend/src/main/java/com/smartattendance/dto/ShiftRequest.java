package com.smartattendance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class ShiftRequest {

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Late-after time is required")
    private LocalTime lateAfter;

    private boolean active = true;

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalTime getLateAfter() {
        return lateAfter;
    }

    public void setLateAfter(LocalTime lateAfter) {
        this.lateAfter = lateAfter;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
