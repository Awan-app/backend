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
            "General", "Work", "Meetings", "Communication", "Personal",
            "Family", "Social", "Health", "Fitness", "Medical",
            "Mental Health", "Learning", "Study", "Reading", "Courses",
            "Finance", "Bills & Payments", "Shopping", "Errands", "Home",
            "Chores", "Home Maintenance", "Cooking & Meal Prep", "Groceries", "Travel",
            "Projects", "Development", "Design", "Content Creation", "Marketing",
            "Research", "Hobbies"
        );

        for (String name : defaultCategories) {
            categoryRepository.save(Category.builder().name(name).build());
        }

        log.info("Seeded {} default categories", defaultCategories.size());
    }
}
