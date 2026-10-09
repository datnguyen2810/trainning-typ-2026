package vn.xuandat.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.xuandat.backend.dto.request.CreateCategoryRequest;
import vn.xuandat.backend.dto.request.UpdateCategoryRequest;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.dto.response.PageResponse;
import vn.xuandat.backend.entity.Category;
import vn.xuandat.backend.exception.ConflictException;
import vn.xuandat.backend.exception.ErrorCode;
import vn.xuandat.backend.exception.ResourceNotFoundException;
import vn.xuandat.backend.mapper.CategoryMapper;
import vn.xuandat.backend.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final List<Category> categories = new ArrayList<>();
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);
        checkSlugAvailable(normalizedSlug, null);

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(normalizedSlug)
                .description(normalizeDescription(request.getDescription()))
                .build();
        
        category.initializeInMemory();
        categories.add(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        for(int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);

            if(category.getId().equals(id)) {
                String slug = request.getSlug().trim().toLowerCase(Locale.ROOT);
                checkSlugAvailable(slug, id);

                category.setName(request.getName().trim());
                category.setDescription(normalizeDescription(request.getDescription()));
                category.setSlug(slug);
                category.setActive(request.getActive());
                category.markUpdatedInMemory();
                categories.set(i, category);

                return categoryMapper.toResponse(category);
            }
        }

        throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found with id: " + id);
    }

    @Override
    public void deleteCategory(UUID id) {
        for (Category category : categories) {
            if(category.getId().equals(id)) {
                categories.remove(category);
                return;
            }
        }

        throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found with id: " + id);
    }

    @Override
    public PageResponse<CategoryResponse> getCategories(int page, int size) {
        List<CategoryResponse> result = new ArrayList<>();

        int start = (page - 1) * size;
        int end = Math.min(start + size, categories.size());
        int totalElements = categories.size();
        int totalPages = (int)Math.ceil(1.0 * totalElements / size);

        if(start >= categories.size()) {
            return PageResponse.<CategoryResponse>builder()
                    .data(result)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }

        for(int i = start; i < end; i++) {
            result.add(categoryMapper.toResponse(categories.get(i)));
        }

        return PageResponse.<CategoryResponse>builder()
                .data(result)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        for (Category category : categories) {
            if (category.getId().equals(id)) {
                return categoryMapper.toResponse(category);
            }
        }
        throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found with id: " + id);
    }

    private void checkSlugAvailable(String slug, UUID id) {
        for (Category category : categories) {
            if (category.getSlug().equals(slug) && !category.getId().equals(id)) {
                throw new ConflictException(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                        "Category slug already exists: " + slug);
            }
        }

    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
