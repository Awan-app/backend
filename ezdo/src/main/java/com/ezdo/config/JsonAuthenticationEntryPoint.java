package com.ezdo.config;

import com.ezdo.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires when an unauthenticated request hits a protected endpoint
 * (missing/invalid JWT, or the token was rejected upstream).
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonAuthErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String authError = (String) request.getAttribute("authError");
        if (ErrorCodes.TOKEN_EXPIRED.equals(authError)) {
            errorWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Access token expired",
                ErrorCodes.TOKEN_EXPIRED
            );
        } else if (ErrorCodes.TOKEN_INVALID.equals(authError)) {
            errorWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid or malformed access token",
                ErrorCodes.TOKEN_INVALID
            );
        } else {
            errorWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication required",
                ErrorCodes.AUTHENTICATION_FAILED
            );
        }
    }
}