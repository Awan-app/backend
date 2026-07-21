package com.ezdo.service;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.InvalidSessionTimeRangeException;
import com.ezdo.exception.SessionLockedException;
import com.ezdo.exception.SessionNotFoundException;
import com.ezdo.mapper.SessionMapper;
import com.ezdo.repository.SessionRepository;
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
    private final SessionMapper sessionMapper;

    @Transactional(readOnly = true)
    public List<SessionResponse> getByTask(UUID taskId, UUID userId) {
        return sessionRepository.findByTaskIdAndUserId(taskId, userId).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getByZone(UUID zoneId, UUID userId) {
        return sessionRepository.findByZoneIdAndUserId(zoneId, userId).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getById(UUID userId, UUID sessionId) {
        return sessionMapper.toResponse(sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId)));
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
        return sessionMapper.toResponse(session);
    }

    public SessionResponse updateStatus(UUID userId, UUID sessionId, SessionStatus status) {
        Session session =  sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.isLocked()) {
            throw new SessionLockedException(session.getId());
        }
        session.setStatus(status);
        return sessionMapper.toResponse(session);
    }

    public SessionResponse lock(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        session.setLocked(true);
        return sessionMapper.toResponse(session);
    }

    public SessionResponse unlock(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        session.setLocked(false);
        return sessionMapper.toResponse(session);
    }

    public void delete(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
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
}
