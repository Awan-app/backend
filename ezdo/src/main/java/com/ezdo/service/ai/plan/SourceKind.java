package com.ezdo.service.ai.plan;

/**
 * Where the planner's source text came from. The planning contract is identical
 * either way — this only picks how the text is framed in the user message.
 */
public enum SourceKind {

    /** A note the user typed into the app. */
    TYPED_NOTE,

    /** The report the vision model produced from an uploaded image. */
    IMAGE_REPORT
}
