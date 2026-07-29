package com.ezdo.service;

import com.ezdo.dto.ai.schedule.AiSchedulingPayload;
import com.ezdo.dto.ai.schedule.AiSchedulingResult;

public interface AiSchedulerClient {
    AiSchedulingResult scheduleTasks(AiSchedulingPayload payload);
}
