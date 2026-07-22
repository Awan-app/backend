package com.ezdo.mapper;

import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.Session;

import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(Session s) {
        return new SessionResponse(
            s.getId(),
            s.getStart(),
            s.getEnd(),
            s.getStatus(),
            s.isLocked(),
            s.getZone() != null ? s.getZone().getId() : null,
            s.getTask().getId()
        );
    }
}
