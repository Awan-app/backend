package com.ezdo.dto.ai;

import java.util.List;

/*
* For parsing purposes...
* */
public record ConversationMessage(
    String role,
    List<ContentBlock> blocks
) {
}
