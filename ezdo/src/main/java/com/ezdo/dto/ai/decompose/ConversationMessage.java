package com.ezdo.dto.ai.decompose;

import java.util.List;

/*
* For parsing purposes...
* */
public record ConversationMessage(
    String role,
    List<ContentBlock> blocks
) {
}
