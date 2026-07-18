package com.ezdo.service;

import com.ezdo.dto.CreateTemplateRequest;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.entity.Template;
import com.ezdo.entity.User;
import com.ezdo.exception.DayInvalidException;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.TemplateRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    public TemplateResponse createTemplate(UUID userId , CreateTemplateRequest createTemplateRequest){

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (templateRepository.existsByUserIdAndDaysOfWeekIn(
                userId,
                createTemplateRequest.daysOfWeek())) {

            throw new DayInvalidException(createTemplateRequest.daysOfWeek());
        }

        Template template = Template.builder()
                .name(createTemplateRequest.name())
                .daysOfWeek(createTemplateRequest.daysOfWeek())
                .user(user)
                .build();

       return toResponse(templateRepository.save(template));

    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplatesByUser(UUID userId) {
        return templateRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID templateId) {
       Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        return toResponse(template);
    }

    public TemplateResponse updateTemplate( UUID userId, UUID templateId, UpdateTemplateRequest request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(()-> new TemplateNotFoundException(templateId));

        if (templateRepository.existsByUserIdAndIdNotAndDaysOfWeekIn(
                userId ,
                templateId ,
                request.daysOfWeek())) {

            throw new DayInvalidException(request.daysOfWeek());
        }

        template.setName(request.name());
        template.setDaysOfWeek(request.daysOfWeek());
        return toResponse(template);
    }

    public void deleteTemplate(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                        .orElseThrow(()-> new TemplateNotFoundException(templateId));
        templateRepository.delete(template);
    }

    private TemplateResponse toResponse(Template t) {
        return new TemplateResponse(t.getId(),
                t.getName(),
                t.getDaysOfWeek(),
                t.getZones().stream().map(ZoneMapper::toZoneResponse).toList());
    }
}
