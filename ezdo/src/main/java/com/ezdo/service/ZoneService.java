package com.ezdo.service;

import com.ezdo.dto.ZoneRequest;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Template;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.Zone;
import com.ezdo.exception.InvalidZoneTimeRangeException;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.TemplateOverrideNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.TemplateOverrideRepository;
import com.ezdo.repository.TemplateRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final TemplateRepository templateRepository;
    private final TemplateOverrideRepository templateOverrideRepository;
    private final ZoneMapper zoneMapper;

    public ZoneResponse addZoneToTemplate(UUID userId, UUID templateId, ZoneRequest request) {
        Template template = templateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(()->new TemplateNotFoundException(templateId));
        validateTimeRange(request.startTime(), request.endTime());

        Zone zone = Zone.builder()
                .name(request.name())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .color(request.color())
                .template(template)
                .build();

        return zoneMapper.toZoneResponse(zoneRepository.save(zone));

    }

    public ZoneResponse addZoneToOverride(UUID userId, UUID overrideId, ZoneRequest request) {
        TemplateOverride override = templateOverrideRepository.findByIdAndUserId(overrideId, userId)
                .orElseThrow(()-> new TemplateOverrideNotFoundException(overrideId));
        validateTimeRange(request.startTime(), request.endTime());

        Zone zone = Zone.builder()
                .name(request.name())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .color(request.color())
                .templateOverride(override)
                .build();

        return zoneMapper.toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByTemplate(UUID templateId) {
        return zoneRepository.findByTemplateId(templateId).stream()
                .map(zoneMapper::toZoneResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByOverride(UUID overrideId) {
        return zoneRepository.findByTemplateOverrideId(overrideId).stream()
                .map(zoneMapper::toZoneResponse)
                .toList();
    }

    public List<ZoneResponse> getZonesByDate(UUID userId, LocalDate date) {

        Optional<TemplateOverride> override =
                templateOverrideRepository.findByUserIdAndDateOfDayWithZones(userId, date);

        if (override.isPresent()) {
            return override.get().getZones().stream()
                    .map(zoneMapper::toZoneResponse)
                    .toList();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        Template template = templateRepository
                .findByUserIdAndDayOfWeekWithZones(userId, dayOfWeek)
                .orElseThrow(() -> new ZoneNotFoundException(userId));

        return template.getZones().stream()
                .map(zoneMapper::toZoneResponse)
                .toList();
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

        zone.setName(request.name());
        zone.setStartTime(request.startTime());
        zone.setEndTime(request.endTime());
        zone.setColor(request.color());
        return zoneMapper.toZoneResponse(zone);
    }

    public void delete(UUID userId, UUID zoneId) {
        zoneRepository.delete(zoneRepository.findByIdAndUserId(zoneId, userId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId)));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidZoneTimeRangeException(start, end);
        }
    }

}
