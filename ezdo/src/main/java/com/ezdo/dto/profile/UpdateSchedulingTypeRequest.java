package com.ezdo.dto.profile;

import com.ezdo.entity.SchedulingType;

public record UpdateSchedulingTypeRequest(
    SchedulingType schedulingType
) {
    public UpdateSchedulingTypeRequest {
        if (schedulingType == null) {
            schedulingType = SchedulingType.BALANCED;
        }
    }
}
