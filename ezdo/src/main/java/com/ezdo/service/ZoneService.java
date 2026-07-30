package com.ezdo.service;

import com.ezdo.dto.ZoneRequest;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Category;
import com.ezdo.entity.Template;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.User;
import com.ezdo.entity.Zone;
import com.ezdo.exception.InvalidZoneTimeRangeException;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.TemplateOverrideNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.exception.ZoneOverlapException;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.TemplateOverrideRepository;
import com.ezdo.repository.TemplateRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final TemplateRepository templateRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateOverrideRepository templateOverrideRepository;
    private final UserRepository userRepository;
    private final ZoneMapper zoneMapper;

    public ZoneResponse addZoneToTemplate(UUID userId, UUID templateId, ZoneRequest request) {
        Template template = templateRepository.findByIdAndUserId(templateId, userId)
            .orElseThrow(() -> new TemplateNotFoundException(templateId));

        validateTimeRange(request.startTime(), request.endTime());
        validateNoOverlap(userId, request.startTime(), request.endTime(), templateId, null);

        Zone zone = Zone.builder()
            .name(request.name())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .color(request.color())
            .category(resolveCategory(userId, request.name()))
            .template(template)
            .build();

        return zoneMapper.toZoneResponse(zoneRepository.save(zone));
    }

    public ZoneResponse addZoneToOverride(UUID userId, UUID overrideId, ZoneRequest request) {
        TemplateOverride override = templateOverrideRepository.findByIdAndUserId(overrideId, userId)
            .orElseThrow(() -> new TemplateOverrideNotFoundException(overrideId));

        validateTimeRange(request.startTime(), request.endTime());
        validateNoOverlap(userId, request.startTime(), request.endTime(), null, overrideId);

        Zone zone = Zone.builder()
            .name(request.name())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .color(request.color())
            .category(resolveCategory(userId, request.name()))
            .templateOverride(override)
            .build();

        return zoneMapper.toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByTemplate(UUID userId, UUID templateId) {
        List<Zone> zones = zoneRepository.findByTemplateIdAndTemplateUserId(templateId, userId);

        if (zones.isEmpty() && !templateRepository.existsById(templateId)) {
            throw new TemplateNotFoundException(templateId);
        }

        return zones.stream()
            .map(zoneMapper::toZoneResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByTemplateOverride(UUID userId, UUID overrideId) {
        List<Zone> zones = zoneRepository.findByTemplateOverrideIdAndTemplateOverrideUserId(overrideId, userId);

        if (zones.isEmpty() && !templateOverrideRepository.existsById(overrideId)) {
            throw new TemplateOverrideNotFoundException(overrideId);
        }

        return zones.stream()
            .map(zoneMapper::toZoneResponse)
            .toList();
    }

    public List<ZoneResponse> getZonesByDate(UUID userId, LocalDate date) {
        return getZonesByDateRange(userId, date, date).getOrDefault(date, List.of());
    }

    /**
     * Resolves zones for every date in a range with a fixed two queries, rather than
     * the two-per-day (plus a lazy category load per zone) that calling
     * {@link #getZonesByDate} in a loop costs. Callers scanning a horizon — the AI
     * schedule context, availability over a range — should use this.
     *
     * <p>Resolution per date is unchanged: a per-date override wins outright,
     * otherwise the template covering that weekday, otherwise nothing.
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, List<ZoneResponse>> getZonesByDateRange(UUID userId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate) {
        Map<LocalDate, TemplateOverride> overridesByDate = templateOverrideRepository
            .findByUserIdAndDateRangeWithZones(userId, startDate, endDate).stream()
            .collect(Collectors.toMap(TemplateOverride::getDateOfDay, o -> o, (first, second) -> first));

        // At most one template may claim a given weekday; the overlap checks on
        // create/update enforce that, so the first one wins here as it does today.
        Map<DayOfWeek, Template> templatesByDay = new EnumMap<>(DayOfWeek.class);
        for (Template template : templateRepository.findByUserIdWithZones(userId)) {
            for (DayOfWeek day : template.getDaysOfWeek()) {
                templatesByDay.putIfAbsent(day, template);
            }
        }

        Map<LocalDate, List<ZoneResponse>> result = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            TemplateOverride override = overridesByDate.get(date);
            List<Zone> zones;
            if (override != null) {
                zones = override.getZones();
            } else {
                Template template = templatesByDay.get(date.getDayOfWeek());
                zones = template != null ? template.getZones() : List.of();
            }
            result.put(date, zones.stream()
                .map(zoneMapper::toZoneResponse)
                .sorted(Comparator.comparing(ZoneResponse::startTime))
                .toList());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ZoneResponse getById(UUID userId, UUID zoneId) {
        return zoneMapper.toZoneResponse(zoneRepository.findByIdAndUserId(zoneId, userId)
            .orElseThrow(() -> new ZoneNotFoundException(zoneId)));
    }

    public ZoneResponse update(UUID userId, UUID zoneId, ZoneRequest request) {
        Zone zone = zoneRepository.findByIdAndUserId(zoneId, userId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

        validateTimeRange(request.startTime(), request.endTime());
        if (zone.getTemplate() != null) {
            validateNoOverlap(userId, request.startTime(), request.endTime(), zone.getTemplate().getId(), null, zoneId);
        } else if (zone.getTemplateOverride() != null) {
            validateNoOverlap(userId, request.startTime(), request.endTime(), null, zone.getTemplateOverride().getId(), zoneId);
        }

        zone.setName(request.name());
        zone.setStartTime(request.startTime());
        zone.setEndTime(request.endTime());
        zone.setColor(request.color());
        zone.setCategory(resolveCategory(userId, request.name()));
        return zoneMapper.toZoneResponse(zone);
    }

    public void delete(UUID userId, UUID zoneId) {
        zoneRepository.delete(zoneRepository.findByIdAndUserId(zoneId, userId)
            .orElseThrow(() -> new ZoneNotFoundException(zoneId)));
    }

    private Category resolveCategory(UUID userId, String zoneName) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        return categoryRepository.findByNameAndUserId(zoneName, userId)
            .orElseGet(() -> categoryRepository.save(Category.builder()
                .name(zoneName)
                .user(user)
                .build()));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidZoneTimeRangeException(start, end);
        }
    }

    private void validateNoOverlap(UUID userId, LocalTime startTime, LocalTime endTime, UUID templateId, UUID overrideId) {
        validateNoOverlap(userId, startTime, endTime, templateId, overrideId, null);
    }

    private void validateNoOverlap(UUID userId, LocalTime startTime, LocalTime endTime, UUID templateId, UUID overrideId, UUID excludeZoneId) {
        List<Zone> existingZones;
        if (templateId != null) {
            existingZones = zoneRepository.findByTemplateIdAndTemplateUserId(templateId, userId);
        } else {
            existingZones = zoneRepository.findByTemplateOverrideIdAndTemplateOverrideUserId(overrideId, userId);
        }

        for (Zone existing : existingZones) {
            if (existing.getId().equals(excludeZoneId)) {
                continue;
            }
            if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                throw new ZoneOverlapException(startTime, endTime);
            }
        }
    }
}
