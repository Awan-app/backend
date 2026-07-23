package com.ezdo.service;

import com.ezdo.dto.ai.AiSchedulingPayload;
import com.ezdo.dto.ai.AiSchedulingResult;

public interface AiSchedulerClient {
    AiSchedulingResult scheduleTasks(AiSchedulingPayload payload);
}
