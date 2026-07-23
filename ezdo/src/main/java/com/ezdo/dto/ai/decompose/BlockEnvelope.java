package com.ezdo.dto.ai.decompose;

import java.util.List;

/**
 * The wire shape the model must reply with, and the shape we replay assistant
 * turns back to the model as: {@code { "blocks": [ ... ] }}.
 */
public record BlockEnvelope(
    List<ContentBlock> blocks
) {
}
