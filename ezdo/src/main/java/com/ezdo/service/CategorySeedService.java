package com.ezdo.service;

import com.ezdo.entity.Category;
import com.ezdo.entity.User;
import com.ezdo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gives a brand-new account a handful of categories to start scheduling against,
 * so the first zone or task never has to be created against an empty list.
 * The user is free to add more afterwards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySeedService {

    static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
        "General", "Work", "Personal", "Health",
        "Learning", "Finance", "Home", "Social"
    );

    private final CategoryRepository categoryRepository;

    public void seedDefaults(User user) {
        categoryRepository.saveAll(DEFAULT_CATEGORY_NAMES.stream()
            .map(name -> Category.builder().name(name).user(user).build())
            .toList());

        log.info("Seeded {} default categories for user {}",
            DEFAULT_CATEGORY_NAMES.size(), user.getId());
    }
}
