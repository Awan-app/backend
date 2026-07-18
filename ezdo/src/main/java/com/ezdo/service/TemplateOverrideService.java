package com.ezdo.service;

import com.ezdo.dto.TemplateOverrideRequest;
import com.ezdo.dto.TemplateOverrideResponse;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.entity.Template;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.User;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.TemplateOverrideNotFoundException;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.TemplateOverrideRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateOverrideService {

    private final TemplateOverrideRepository templateOverrideRepository;
    private final UserRepository userRepository;

    public TemplateOverrideResponse create(UUID userId , TemplateOverrideRequest templateOverrideRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new TemplateNotFoundException(userId));

        TemplateOverride override = TemplateOverride.builder().
                name(templateOverrideRequest.name())
                .dateOfDay(templateOverrideRequest.dateOfDay())
                .user(user)
                . build();

        return toResponse(templateOverrideRepository.save(override));
    }

    @Transactional(readOnly = true)
    public List<TemplateOverrideResponse> getByUser(UUID userId ){
        return templateOverrideRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateOverrideResponse getById(UUID overrideId){
        return toResponse(templateOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new TemplateOverrideNotFoundException(overrideId)));

    }

    public TemplateOverrideResponse updateTemplate(UUID overrideId, TemplateOverrideRequest request) {
        TemplateOverride override = templateOverrideRepository.findById(overrideId)
                .orElseThrow(()-> new TemplateNotFoundException(overrideId));

        override.setName(request.name());
        override.setDateOfDay(request.dateOfDay());
        return toResponse(override);
    }

    public void delete(UUID templateOverrideId) {
        templateOverrideRepository.delete(templateOverrideRepository.findById(templateOverrideId)
                .orElseThrow(() -> new TemplateOverrideNotFoundException(templateOverrideId)));
    }

    private TemplateOverrideResponse toResponse(TemplateOverride o) {
        return new TemplateOverrideResponse(o.getId(),
                o.getName(),
                o.getDateOfDay(),
                o.getZones().stream().map(ZoneMapper::toZoneResponse).toList());
    }
}
