package com.ezdo.controller;


import com.ezdo.dto.AvailableSlot;
import com.ezdo.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AvailableSlot>> getAvailableSlots(
            @AuthenticationPrincipal UUID userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(availabilityService.getAvailableSlots(userId, date));
    }

    @GetMapping("/range")
    public ResponseEntity<Map<LocalDate, List<AvailableSlot>>> getAvailableSlotsForRange(
            @AuthenticationPrincipal UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(availabilityService.getAvailableSlotsForRange(userId, startDate, endDate));
    }
}
