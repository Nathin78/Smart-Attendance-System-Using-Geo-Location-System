package com.smartattendance.controller;

import com.smartattendance.dto.ShiftResponse;
import com.smartattendance.service.ShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/me")
    public ResponseEntity<ShiftResponse> getMyShift(Authentication authentication) {
        return ResponseEntity.ok(shiftService.getCurrentShift(authentication.getName()));
    }
}
