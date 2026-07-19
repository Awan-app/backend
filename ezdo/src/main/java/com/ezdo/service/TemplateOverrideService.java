package com.ezdo.service;

import com.ezdo.dto.TemplateOverrideRequest;
import com.ezdo.dto.TemplateOverrideResponse;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.entity.Template;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.User;
import com.ezdo.dto.ZoneRequest;
import com.ezdo.entity.Zone;
import com.ezdo.exception.InvalidZoneTimeRangeException;
import com.ezdo.exception.TemplateNotFoundException;
import com.ezdo.exception.TemplateOverrideNotFoundException;
import com.ezdo.exception.UserNotFoundException;
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
    private final ZoneMapper zoneMapper;

    public TemplateOverrideResponse create(UUID userId , TemplateOverrideRequest templateOverrideRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new UserNotFoundException());

        TemplateOverride override = TemplateOverride.builder().
                name(templateOverrideRequest.name())
                .dateOfDay(templateOverrideRequest.dateOfDay())
                .user(user)
                . build();

        if (templateOverrideRequest.zones() != null) {
            for (ZoneRequest zr : templateOverrideRequest.zones()) {
                if (zr.startTime() == null || zr.endTime() == null || !zr.endTime().isAfter(zr.startTime())) {
                    throw new InvalidZoneTimeRangeException(zr.startTime(), zr.endTime());
                }
                Zone zone = Zone.builder()
                        .name(zr.name())
                        .startTime(zr.startTime())
                        .endTime(zr.endTime())
                        .color(zr.color())
                        .templateOverride(override)
                        .build();
                override.getZones().add(zone);
            }
        }

        return toResponse(templateOverrideRepository.save(override));
    }

    @Transactional(readOnly = true)
    public List<TemplateOverrideResponse> getByUser(UUID userId ){
        return templateOverrideRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateOverrideResponse getById(UUID userId, UUID overrideId){
        return toResponse(templateOverrideRepository.findByIdAndUserId(overrideId, userId)
                .orElseThrow(() -> new TemplateOverrideNotFoundException(overrideId)));

    }

    public TemplateOverrideResponse updateTemplate(UUID userId, UUID overrideId, TemplateOverrideRequest request) {
        TemplateOverride override = templateOverrideRepository.findByIdAndUserId(overrideId, userId)
                .orElseThrow(()-> new TemplateOverrideNotFoundException(overrideId));

        override.setName(request.name());
        override.setDateOfDay(request.dateOfDay());
        return toResponse(override);
    }

    public void delete(UUID userId, UUID templateOverrideId) {
        templateOverrideRepository.delete(templateOverrideRepository.findByIdAndUserId(templateOverrideId, userId)
                .orElseThrow(() -> new TemplateOverrideNotFoundException(templateOverrideId)));
    }

    private TemplateOverrideResponse toResponse(TemplateOverride o) {
        return new TemplateOverrideResponse(o.getId(),
                o.getName(),
                o.getDateOfDay(),
                o.getZones().stream().map(zoneMapper::toZoneResponse).toList());
    }
}
