package com.ezdo.service;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.entity.Zone;
import com.ezdo.exception.InvalidSessionTimeRangeException;
import com.ezdo.exception.SessionLockedException;
import com.ezdo.exception.SessionNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ZoneRepository zoneRepository;

    public SessionResponse createSession(UUID userId, UUID zoneId, SessionRequest request) {
        Zone zone = zoneRepository.findByIdAndUserId(zoneId, userId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        validateTimeRange(request.start(), request.end());

        Session session = Session.builder()
                .start(request.start())
                .end(request.end())
                .status(request.status() != null ? request.status() : SessionStatus.SCHEDULED)
                .zone(zone)
                .build();

        return toResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getByZone(UUID zoneId) {
        return sessionRepository.findByZoneId(zoneId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getById(UUID userId, UUID sessionId) {
        return toResponse(sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId)));
    }

    public SessionResponse update(UUID userId, UUID sessionId, SessionRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));

        if (session.isLocked()) {
            throw new SessionLockedException(session.getId());
        }
        validateTimeRange(request.start(), request.end());

        session.setStart(request.start());
        session.setEnd(request.end());
        if (request.status() != null) session.setStatus(request.status());
        return toResponse(session);
    }

    public SessionResponse updateStatus(UUID userId, UUID sessionId, SessionStatus status) {
        Session session =  sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));

        if (session.isLocked()) {
            throw new SessionLockedException(session.getId());
        }
        session.setStatus(status);
        return toResponse(session);
    }

    public SessionResponse lock(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));
        session.setLocked(true);
        return toResponse(session);
    }

    public SessionResponse unlock(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));
        session.setLocked(false);
        return toResponse(session);
    }

    public void delete(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));
        if (session.isLocked()) {
            throw new SessionLockedException(session.getId());
        }
        sessionRepository.delete(session);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidSessionTimeRangeException(start, end);
        }
    }


    private SessionResponse toResponse(Session s) {
        return new SessionResponse(s.getId(), s.getStart(), s.getEnd(), s.getStatus(), s.isLocked(), s.getZone().getId());
    }
}
