package com.ezdo.service.ai;

/**
 * Pulls the JSON object out of a raw model reply before it reaches Jackson.
 *
 * <p>Every AI contract in this app says "no markdown, no code fences, no prose",
 * and models mostly obey — but not always, and a single stray ```json fence turns
 * an otherwise perfect reply into a parse failure that costs a retry. This strips
 * fences and any surrounding chatter, then takes the outermost {@code { ... }} span.
 */
public final class JsonExtractor {

    /** Raised when the raw text contains no JSON object at all. */
    public static class NoJsonObjectException extends RuntimeException {
        public NoJsonObjectException(String message) {
            super(message);
        }
    }

    private JsonExtractor() {
    }

    public static String extractObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new NoJsonObjectException("Model returned empty output");
        }
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int closingFence = s.lastIndexOf("```");
            if (closingFence >= 0) {
                s = s.substring(0, closingFence);
            }
            s = s.strip();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new NoJsonObjectException("No JSON object found in model output");
        }
        return s.substring(start, end + 1);
    }
}
