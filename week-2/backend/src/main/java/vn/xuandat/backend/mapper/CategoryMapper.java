package vn.xuandat.backend.mapper;

import org.springframework.stereotype.Component;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.entity.Category;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }


}

