package com.ezdo.controller;

import com.ezdo.exception.SomethingNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping
    public ResponseEntity<String> test(
        @RequestParam(required = false) String error
    ) {
        if (error != null) {
            throw new SomethingNotFoundException(UUID.randomUUID().toString());
        }
        return ResponseEntity.ok("Working!!");
    }
}
