package com.ezdo.config;

import com.ezdo.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires when an authenticated user hits an endpoint/method they don't have
 * permission for (e.g. role check failure).
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonAuthErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        errorWriter.write(
            response,
            HttpStatus.FORBIDDEN.value(),
            "You do not have permission to perform this action",
            ErrorCodes.ACCESS_DENIED
        );
    }
}