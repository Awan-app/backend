package com.ezdo.dto;

import lombok.Builder;

@Builder
public record McpConnectionDetailsResponse(
    String mcpUrl,
    String clientId
) {}
