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

import static com.ezdo.mapper.ZoneMapper.toZoneResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final TemplateRepository templateRepository;
    private final TemplateOverrideRepository templateOverrideRepository;

    public ZoneResponse addZoneToTemplate(UUID templateId, ZoneRequest request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(()->new TemplateNotFoundException(templateId));
        validateTimeRange(request.startTime(), request.endTime());

        Zone zone = Zone.builder()
                .name(request.name())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .color(request.color())
                .template(template)
                .build();

        return toZoneResponse(zoneRepository.save(zone));

    }

    public ZoneResponse addZoneToOverride(UUID overrideId, ZoneRequest request) {
        TemplateOverride override = templateOverrideRepository.findById(overrideId)
                .orElseThrow(()-> new TemplateOverrideNotFoundException(overrideId));
        validateTimeRange(request.startTime(), request.endTime());

        Zone zone = Zone.builder()
                .name(request.name())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .color(request.color())
                .templateOverride(override)
                .build();

        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByTemplate(UUID templateId) {
        return zoneRepository.findByTemplateId(templateId).stream()
                .map(ZoneMapper::toZoneResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getByOverride(UUID overrideId) {
        return zoneRepository.findByTemplateOverrideId(overrideId).stream()
                .map(ZoneMapper::toZoneResponse)
                .toList();
    }

    public List<ZoneResponse> getZonesByDate(UUID userId, LocalDate date) {

        Optional<TemplateOverride> override =
                templateOverrideRepository.findByUserIdAndDateOfDayWithZones(userId, date);

        if (override.isPresent()) {
            return override.get().getZones().stream()
                    .map(ZoneMapper::toZoneResponse)
                    .toList();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        Template template = templateRepository
                .findByWeeklyTemplateIdAndDayOfWeekWithZones(userId, dayOfWeek)
                .orElseThrow(() -> new ZoneNotFoundException(userId));

        return template.getZones().stream()
                .map(ZoneMapper::toZoneResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ZoneResponse getById(UUID zoneId) {
        return toZoneResponse(zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId)));
    }

    public ZoneResponse update(UUID zoneId, ZoneRequest request) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

        validateTimeRange(request.startTime(), request.endTime());

        zone.setName(request.name());
        zone.setStartTime(request.startTime());
        zone.setEndTime(request.endTime());
        zone.setColor(request.color());
        return toZoneResponse(zone);
    }

    public void delete(UUID zoneId) {
        zoneRepository.delete(zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId)));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidZoneTimeRangeException(start, end);
        }
    }

}
