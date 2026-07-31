package com.ezdo.service.ai;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.entity.Category;
import com.ezdo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The guardrail every model-produced task draft passes through before it becomes a
 * {@code TaskCreateRequest}: category ids are checked against what the user really
 * owns, and out-of-range numbers are clamped to something the entity will accept.
 * Shared by single-task enrichment and image extraction so the two can never drift.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDraftNormalizer {

    public static final int DEFAULT_ESTIMATED_DURATION_MINUTES = 30;

    private final CategoryRepository categoryRepository;

    /**
     * The set of category ids this user actually owns. Load it once per request and
     * feed it to {@link #sanitizeCategory}, so extracting N tasks costs one query
     * rather than N.
     */
    public Set<UUID> validCategoryIds() {
        return categoryRepository.findAll().stream()
            .map(Category::getId)
            .collect(Collectors.toSet());
    }

    /** Never trust the model's category id blindly — clear it if it doesn't exist. */
    public CategoryResponse sanitizeCategory(CategoryResponse category, Set<UUID> validIds) {
        if (category == null || category.id() == null) {
            return null;
        }
        if (!validIds.contains(category.id())) {
            log.warn("AI draft referenced unknown category id {}; clearing it", category.id());
            return null;
        }
        return category;
    }

    public Integer normalizeDuration(Integer minutes) {
        if (minutes == null || minutes < 1) {
            log.warn("AI draft returned invalid estimatedDuration {}, defaulting to {}",
                minutes, DEFAULT_ESTIMATED_DURATION_MINUTES);
            return DEFAULT_ESTIMATED_DURATION_MINUTES;
        }
        return minutes;
    }

    public Integer normalizePoints(Integer points) {
        if (points == null || points < 0) {
            return 0;
        }
        return points;
    }

    /** Trim a model-produced string to the column width, or null if it's blank. */
    public String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        return stripped.length() <= maxLength ? stripped : stripped.substring(0, maxLength);
    }
}
