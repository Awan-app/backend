package com.ezdo.service;

import com.ezdo.dto.*;
import com.ezdo.entity.TemplateOverride;
import com.ezdo.entity.User;
import com.ezdo.entity.Zone;
import com.ezdo.exception.InvalidZoneTimeRangeException;
import com.ezdo.exception.TemplateOverrideNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.mapper.ZoneMapper;
import com.ezdo.repository.TemplateOverrideRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateOverrideService {

    private final TemplateOverrideRepository templateOverrideRepository;
    private final UserRepository userRepository;
    private final ZoneMapper zoneMapper;
    private final ZoneRepository zoneRepository;

    public TemplateOverrideResponse create(UUID userId , TemplateOverrideRequest templateOverrideRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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

    public List<ZoneResponse> updateTemplateZones(UUID overrideId , UUID userId , UpdateTemplateZoneRequest request){

        TemplateOverride override = templateOverrideRepository.findById(overrideId)
                .orElseThrow(() ->new TemplateOverrideNotFoundException(overrideId));

        List<Zone> existingZones = zoneRepository.findByTemplateOverrideIdAndTemplateOverrideUserId(overrideId ,userId);

        Map<UUID, Zone> existingById = existingZones.stream()
                .collect(Collectors.toMap(Zone::getId, z -> z));

        Set<UUID> keepIds = new HashSet<>();
        List<Zone> toSave = new ArrayList<>();

        for(ZoneRequest item : request.zones()){
            validateTimeRange(item.startTime(), item.endTime());
            if(item.id() == null){
                //add new one
                toSave.add(Zone.builder()
                        .name(item.name())
                        .startTime(item.startTime())
                        .endTime(item.endTime())
                        .color(item.color())
                        .templateOverride(override)
                        .build());
            }
            else{
                //update on existing zone
                Zone existing = existingById.get(item.id());

                if (existing == null) {
                    throw new ZoneNotFoundException(item.id());
                }
                existing.setName(item.name());
                existing.setStartTime(item.startTime());
                existing.setEndTime(item.endTime());
                existing.setColor(item.color());
                toSave.add(existing);
                keepIds.add(item.id());
            }

        }
        // delete
        List<Zone> toDelete = existingZones.stream()
                .filter(z -> !keepIds.contains(z.getId()))
                .toList();
        zoneRepository.deleteAll(toDelete);

        List<Zone> saved = zoneRepository.saveAll(toSave);
        return saved.stream().map(zoneMapper ::toZoneResponse).toList();

    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidZoneTimeRangeException(start, end);
        }
    }

    private TemplateOverrideResponse toResponse(TemplateOverride o) {
        return new TemplateOverrideResponse(o.getId(),
                o.getName(),
                o.getDateOfDay(),
                o.getZones().stream().map(zoneMapper::toZoneResponse).toList());
    }
}
