package com.ezdo.service;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.InvalidOperationException;
import com.ezdo.exception.InvalidSessionTimeRangeException;
import com.ezdo.exception.SessionNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.SessionMapper;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.scheduler.SessionSchedulerService;
import com.ezdo.util.DateRangeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final UserRepository userRepository;
    private final SessionSchedulerService sessionSchedulerService;

    @Transactional(readOnly = true)
    public List<SessionResponse> getByTask(UUID taskId, UUID userId, SessionStatus status) {
        return sessionRepository.findByTaskIdAndUserId(taskId, userId, status).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getByZone(UUID zoneId, UUID userId, SessionStatus status) {
        return sessionRepository.findByZoneIdAndUserId(zoneId, userId, status).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getByDate(UUID userId, LocalDate date, SessionStatus status) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return sessionRepository.findByUserIdAndDateRange(userId, start, end, status).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, List<SessionResponse>> getByDateRange(UUID userId, LocalDate startDate, LocalDate endDate, SessionStatus status) {
        DateRangeValidator.validate(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<SessionResponse> sessions = sessionRepository.findByUserIdAndDateRange(userId, start, end, status).stream()
                .map(sessionMapper::toResponse)
                .toList();

        return sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.start().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional(readOnly = true)
    public SessionResponse getById(UUID userId, UUID sessionId) {
        return sessionMapper.toResponse(sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId)));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getMissedOnDate(UUID userId, LocalDate date) {
        LocalDateTime now = resolveNow(userId);
        return sessionRepository.findMissed(userId, now,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getMissedRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        DateRangeValidator.validate(startDate, endDate);
        LocalDateTime now = resolveNow(userId);
        return sessionRepository.findMissed(userId, now,
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    public SessionResponse update(UUID userId, UUID sessionId, SessionRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(()->new SessionNotFoundException(sessionId));

        validateTimeRange(request.start(), request.end());

        session.setStart(request.start());
        session.setEnd(request.end());
        if (request.status() != null) {
            validateTransition(session.getStatus(), request.status());
            session.setStatus(request.status());
        }
        syncReminder(userId, session);
        return sessionMapper.toResponse(session);
    }

    public SessionResponse updateStatus(UUID userId, UUID sessionId, SessionStatus status) {
        Session session =  sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        validateTransition(session.getStatus(), status);
        session.setStatus(status);
        syncReminder(userId, session);
        return sessionMapper.toResponse(session);
    }

    public SessionResponse complete(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        validateTransition(session.getStatus(), SessionStatus.COMPLETED);
        session.setStatus(SessionStatus.COMPLETED);
        sessionSchedulerService.cancelReminder(session.getId());
        return sessionMapper.toResponse(session);
    }

    public SessionResponse uncomplete(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        validateTransition(session.getStatus(), SessionStatus.SCHEDULED);
        session.setStatus(SessionStatus.SCHEDULED);
        sessionSchedulerService.scheduleReminder(session, userId);
        return sessionMapper.toResponse(session);
    }

    public SessionResponse cancel(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        validateTransition(session.getStatus(), SessionStatus.CANCELLED);
        session.setStatus(SessionStatus.CANCELLED);
        sessionSchedulerService.cancelReminder(session.getId());
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
        sessionSchedulerService.cancelReminder(session.getId());
        sessionRepository.delete(session);
    }

    /**
     * Keeps the reminder job aligned with the session's current state: scheduled
     * sessions get a reminder, anything else (completed/cancelled) has its
     * reminder removed.
     */
    private void syncReminder(UUID userId, Session session) {
        if (session.getStatus() == SessionStatus.SCHEDULED) {
            sessionSchedulerService.scheduleReminder(session, userId);
        } else {
            sessionSchedulerService.cancelReminder(session.getId());
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidSessionTimeRangeException(start, end);
        }
    }

    private LocalDateTime resolveNow(UUID userId) {
        Preferences prefs = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)).getPreferences();
        ZoneId zone = (prefs != null && prefs.getTimezone() != null)
                ? ZoneId.of(prefs.getTimezone())
                : ZoneId.of("UTC");
        return LocalDateTime.now(zone);
    }

    private void validateTransition(SessionStatus current, SessionStatus next) {
        if (current == next) {
            return;
        }
        boolean valid = switch (current) {
            case SCHEDULED -> next == SessionStatus.COMPLETED || next == SessionStatus.CANCELLED;
            case COMPLETED -> next == SessionStatus.SCHEDULED;
            case CANCELLED -> false;
        };
        if (!valid) {
            throw new InvalidOperationException(
                "Cannot transition session from " + current + " to " + next);
        }
    }
}
