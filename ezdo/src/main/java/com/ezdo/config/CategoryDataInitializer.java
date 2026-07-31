package com.ezdo.config;

import com.ezdo.entity.Category;
import com.ezdo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<String> defaultCategories = List.of(
            "Work", "Personal", "Health", "Learning", "Finance",
            "Development", "Design", "Meetings", "Fitness", "Reading"
        );

        for (String name : defaultCategories) {
            categoryRepository.save(Category.builder().name(name).build());
        }

        log.info("Seeded {} default categories", defaultCategories.size());
    }
}
