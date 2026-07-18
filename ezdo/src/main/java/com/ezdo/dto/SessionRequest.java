package com.ezdo.dto;

import com.ezdo.entity.SessionStatus;

import java.time.LocalDateTime;

public record SessionRequest(
        LocalDateTime start ,
        LocalDateTime end ,
        SessionStatus status
) {
}
