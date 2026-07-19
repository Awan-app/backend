package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class TemplateNotFoundException extends ApplicationException{

    public TemplateNotFoundException(UUID templateId) {
        super(
                "Template with id=" + templateId + " not found",
                404,
                ErrorCodes.TEMPLATE_NOT_FOUND,
                Map.of(
                        "templateId", templateId
                )
        );
    }
}
