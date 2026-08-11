package com.ezdo.controller;

import com.ezdo.dto.McpConnectionDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mcp/settings")
@RequiredArgsConstructor
public class McpSettingsController {

    @Value("${ezdo.mcp.base-url}")
    private String mcpUrl;

    @Value("${ezdo.mcp.client-id}")
    private String clientId;

    @GetMapping("/connection-details")
    public ResponseEntity<McpConnectionDetailsResponse> getConnectionDetails() {
        return ResponseEntity.ok(
            McpConnectionDetailsResponse.builder()
                .mcpUrl(mcpUrl)
                .clientId(clientId)
                .build()
        );
    }
}
