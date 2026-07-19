package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class TemplateOverrideNotFoundException extends ApplicationException{

    public TemplateOverrideNotFoundException(UUID templateOverrideId) {
        super(
                "TemplateOverride with id=" + templateOverrideId + " not found",
                404,
                ErrorCodes.TEMPLATE_OVERRIDE_NOT_FOUND,
                Map.of(
                        "templateOverrideId", templateOverrideId
                )
        );
    }
}
