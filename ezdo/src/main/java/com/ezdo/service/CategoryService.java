package com.ezdo.service;

import com.ezdo.dto.CategoryRequest;
import com.ezdo.dto.CategoryResponse;
import com.ezdo.entity.Category;
import com.ezdo.entity.User;
import com.ezdo.exception.CategoryNotFoundException;
import com.ezdo.exception.DuplicateCategoryNameException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.CategoryMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    public CategoryResponse create(UUID userId, CategoryRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        Category category = Category.builder()
            .name(request.name())
            .user(user)
            .build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(UUID userId) {
        return categoryRepository.findByUserId(userId).stream()
            .map(categoryMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID userId, UUID categoryId) {
        return categoryMapper.toResponse(categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId)));
    }

    public CategoryResponse update(UUID userId, UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        // A pure-case rename ("work" -> "Work") must not collide with itself.
        if (!category.getName().equalsIgnoreCase(request.name())
            && categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        category.setName(request.name());
        return categoryMapper.toResponse(category);
    }
}
