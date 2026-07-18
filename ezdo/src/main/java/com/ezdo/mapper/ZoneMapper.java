package com.ezdo.mapper;

import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Zone;

public class ZoneMapper {

    private ZoneMapper() {}

    public static ZoneResponse toZoneResponse(Zone z) {
        return new ZoneResponse(
                z.getId(),
                z.getName(),
                z.getStartTime(),
                z.getEndTime(),
                z.getColor(),
                z.getTemplate() != null ? z.getTemplate().getId() : null,
                z.getTemplateOverride() != null ? z.getTemplateOverride().getId() : null
        );
    }
}
