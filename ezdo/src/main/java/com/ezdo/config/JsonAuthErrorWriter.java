package com.ezdo.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonAuthErrorWriter {

    private final JsonMapper jsonMapper;

    public JsonAuthErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletResponse response, int status, String message, String errorCode) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("statusCode", status);
        body.put("errorCode", errorCode);
        body.put("info", Map.of());
        body.put("timestamp", LocalDateTime.now());

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}