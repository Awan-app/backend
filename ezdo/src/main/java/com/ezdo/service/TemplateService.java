package com.ezdo.service;

import com.ezdo.dto.CreateTemplateRequest;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.entity.Template;
import com.ezdo.entity.User;
import com.ezdo.dto.ZoneRequest;
import com.ezdo.entity.Zone;
import com.ezdo.exception.DayInvalidException;
import com.ezdo.exception.InvalidZoneTimeRangeException;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import java.time.LocalTime;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.TemplateRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneMapper zoneMapper;

    public TemplateResponse createTemplate(UUID userId, CreateTemplateRequest request){
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.daysOfWeek() != null && !request.daysOfWeek().isEmpty()) {
            if (templateRepository.existsByUserIdAndDaysOfWeekIn(userId, request.daysOfWeek())) {
                throw new DayInvalidException(request.daysOfWeek());
            }
        }

        Template template = Template.builder()
            .name(request.name())
            .daysOfWeek(request.daysOfWeek() != null ? request.daysOfWeek() : Set.of())
            .user(user)
            .build();

        if (request.zones() != null) {
            for (ZoneRequest zr : request.zones()) {
                validateTimeRange(zr.startTime(), zr.endTime());
                Zone zone = Zone.builder()
                    .name(zr.name())
                    .startTime(zr.startTime())
                    .endTime(zr.endTime())
                    .color(zr.color())
                    .category(zr.categoryId() != null ? categoryRepository.findById(zr.categoryId()).orElse(null) : null)
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
        template.setDaysOfWeek(request.daysOfWeek());
        return toResponse(template);
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

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidZoneTimeRangeException(start, end);
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
