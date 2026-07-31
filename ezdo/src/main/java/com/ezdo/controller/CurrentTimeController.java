package com.ezdo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/time")
public class CurrentTimeController {

    @GetMapping
    public ResponseEntity<LocalDateTime> currentTime() {
        return ResponseEntity.ok(LocalDateTime.now());
    }
}
