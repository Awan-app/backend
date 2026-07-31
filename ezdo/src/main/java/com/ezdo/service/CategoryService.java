package com.ezdo.service;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.entity.Category;
import com.ezdo.exception.CategoryNotFoundException;
import com.ezdo.mapper.CategoryMapper;
import com.ezdo.repository.CategoryRepository;
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
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
            .map(categoryMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID categoryId) {
        return categoryMapper.toResponse(categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId)));
    }
}
