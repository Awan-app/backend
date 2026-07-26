package com.ezdo.service;

import com.ezdo.dto.CreateTemplateRequest;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.entity.Category;
import com.ezdo.dto.*;
import com.ezdo.entity.Template;
import com.ezdo.entity.User;
import com.ezdo.entity.Zone;
import com.ezdo.exception.*;

import java.time.LocalTime;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.TemplateRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneMapper zoneMapper;
    private final ZoneRepository zoneRepository;
    private final SessionRepository sessionRepository;

    public TemplateResponse createTemplate(UUID userId, CreateTemplateRequest request){
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.daysOfWeek() != null && !request.daysOfWeek().isEmpty()) {
            if (templateRepository.existsByUserIdAndDaysOfWeekIn(userId, request.daysOfWeek())) {
                throw new DayInvalidException(request.daysOfWeek());
            }
        }

        if (request.zones() != null) {
            for (ZoneRequest zr : request.zones()) {
                validateTimeRange(zr.startTime(), zr.endTime());
            }
            validateZonesNoOverlap(request.zones());
        }

        Template template = Template.builder()
            .name(request.name())
            .daysOfWeek(request.daysOfWeek() != null ? request.daysOfWeek() : Set.of())
            .user(user)
            .build();

        if (request.zones() != null) {
            for (ZoneRequest zr : request.zones()) {
                Zone zone = Zone.builder()
                    .name(zr.name())
                    .startTime(zr.startTime())
                    .endTime(zr.endTime())
                    .color(zr.color())
                    .category(resolveCategory(userId, zr.name()))
                    .template(template)
                    .build();
                template.getZones().add(zone);
            }
        }

       return toResponse(templateRepository.save(template));

    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplatesByUser(UUID userId) {
        return templateRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID userId, UUID templateId) {
       Template template = templateRepository.findByIdAndUserId(templateId, userId)
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
        return toResponse(template);
    }

    public TemplateResponse updateTemplate( UUID userId, UUID templateId, UpdateTemplateRequest request) {
        Template template = templateRepository.findByIdAndUserId(templateId, userId)
            .orElseThrow(()-> new TemplateNotFoundException(templateId));

        if (templateRepository.existsByUserIdAndIdNotAndDaysOfWeekIn(
            userId,
            templateId,
            request.daysOfWeek())
        ) {
            throw new DayInvalidException(request.daysOfWeek());
        }

        template.setName(request.name());
        template.setDaysOfWeek(request.daysOfWeek() != null ? request.daysOfWeek() : Set.of());
        return toResponse(template);
    }

    public List<ZoneResponse> updateTemplateZones(UUID templateId, UUID userId, UpdateTemplateZoneRequest request) {

        Template template = templateRepository.findByIdAndUserId(templateId, userId)
            .orElseThrow(() -> new TemplateNotFoundException(templateId));

        List<Zone> existingZones = zoneRepository.findByTemplateIdAndTemplateUserId(templateId, userId);

        Map<UUID, Zone> existingById = existingZones.stream()
            .collect(Collectors.toMap(Zone::getId, z -> z));

        Set<UUID> keepIds = new HashSet<>();
        List<Zone> toSave = new ArrayList<>();

        for (ZoneRequest item : request.zones()) {
            validateTimeRange(item.startTime(), item.endTime());
            if (item.id() == null) {
                //add new one
                toSave.add(Zone.builder()
                    .name(item.name())
                    .startTime(item.startTime())
                    .endTime(item.endTime())
                    .color(item.color())
                    .category(resolveCategory(userId, item.name()))
                    .template(template)
                    .build());
            } else {
                //update on existing zone
                Zone existing = existingById.get(item.id());

                if (existing == null) {
                    throw new ZoneNotFoundException(item.id());
                }
                existing.setName(item.name());
                existing.setStartTime(item.startTime());
                existing.setEndTime(item.endTime());
                existing.setColor(item.color());
                existing.setCategory(resolveCategory(userId, item.name()));
                toSave.add(existing);
                keepIds.add(item.id());
            }

        }
        // delete
        List<Zone> toDelete = existingZones.stream()
            .filter(z -> !keepIds.contains(z.getId()))
            .toList();
        if (!toDelete.isEmpty()) {
            sessionRepository.nullifyZoneId(
                toDelete.stream().map(Zone::getId).toList());
        }
        zoneRepository.deleteAll(toDelete);

        List<Zone> saved = zoneRepository.saveAll(toSave);
        return saved.stream().map(zoneMapper::toZoneResponse).toList();
    }

    public void deleteTemplate(UUID userId, UUID templateId) {
        Template template = templateRepository.findByIdAndUserId(templateId, userId)
            .orElseThrow(()-> new TemplateNotFoundException(templateId));
        templateRepository.delete(template);
    }

//    private void reassignDays(UUID userId, java.util.Set<java.time.DayOfWeek> days, UUID excludeTemplateId) {
//        List<Template> conflictingTemplates = templateRepository.findTemplatesWithConflictingDays(userId, days);
//        for (Template t : conflictingTemplates) {
//            if (excludeTemplateId != null && t.getId().equals(excludeTemplateId)) {
//                continue;
//            }
//            t.getDaysOfWeek().removeAll(days);
//            if (t.getDaysOfWeek().isEmpty()) {
//                templateRepository.delete(t);
//            }
//        }
//    }

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

    private void validateZonesNoOverlap(List<ZoneRequest> zones) {
        for (int i = 0; i < zones.size(); i++) {
            for (int j = i + 1; j < zones.size(); j++) {
                ZoneRequest a = zones.get(i);
                ZoneRequest b = zones.get(j);
                if (a.startTime().isBefore(b.endTime()) && a.endTime().isAfter(b.startTime())) {
                    throw new ZoneOverlapException(a.startTime(), a.endTime());
                }
            }
        }
    }

    private TemplateResponse toResponse(Template t) {
        return new TemplateResponse(t.getId(),
            t.getName(),
            t.getDaysOfWeek(),
            t.getZones().stream()
                .map(zoneMapper::toZoneResponse)
                .toList()
        );
    }
}
