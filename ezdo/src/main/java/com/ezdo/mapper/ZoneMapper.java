package com.ezdo.mapper;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Zone;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZoneMapper {

    private final CategoryMapper categoryMapper;

    public ZoneResponse toZoneResponse(Zone z) {
        CategoryResponse category = z.getCategory() != null
            ? categoryMapper.toResponse(z.getCategory()) : null;

        return new ZoneResponse(
                z.getId(),
                z.getName(),
                z.getStartTime(),
                z.getEndTime(),
                z.getColor(),
                category,
                z.getTemplate() != null ? z.getTemplate().getId() : null,
                z.getTemplateOverride() != null ? z.getTemplateOverride().getId() : null
        );
    }
}
