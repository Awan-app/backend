package com.ezdo.service.ai.image;

import com.ezdo.dto.ai.image.ImageExtractionResult;
import com.ezdo.exception.ResultParseException;
import com.ezdo.service.ai.JsonExtractor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The single place that knows how to turn the planning model's raw reply into an
 * {@link ImageExtractionResult}. Wraps the Spring-Boot-autoconfigured
 * {@link ObjectMapper} and strips markdown fences/surrounding prose via
 * {@link JsonExtractor}, the same way the other AI codecs do.
 */
@Component
public class ImageTaskExtractionCodec {

    private final ObjectMapper objectMapper;

    public ImageTaskExtractionCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImageExtractionResult parseResult(String rawModelOutput) {
        if (rawModelOutput == null || rawModelOutput.isBlank()) {
            throw new ResultParseException("Model returned empty output", null);
        }
        try {
            return objectMapper.readValue(
                rawModelOutput, ImageExtractionResult.class);
        } catch (Exception e) {
            throw new ResultParseException("Model output was not a valid extraction result", e);
        }
    }
}
