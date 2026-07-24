package com.ezdo.mapper;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
            c.getId(),
            c.getName());
    }
}
