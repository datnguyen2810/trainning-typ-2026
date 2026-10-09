package vn.xuandat.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.xuandat.backend.dto.request.CreateCategoryRequest;
import vn.xuandat.backend.dto.request.UpdateCategoryRequest;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.dto.response.PageResponse;
import vn.xuandat.backend.entity.Category;
import vn.xuandat.backend.exception.ConflictException;
import vn.xuandat.backend.exception.ErrorCode;
import vn.xuandat.backend.exception.ResourceNotFoundException;
import vn.xuandat.backend.mapper.CategoryMapper;
import vn.xuandat.backend.repository.CategoryRepository;
import vn.xuandat.backend.service.CategoryService;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedSlug = request.getSlug().toLowerCase(Locale.ROOT);

        if(categoryRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists with by slug : " +  normalizedSlug
            );
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(normalizedSlug)
                .description(normalizeDescription(request.getDescription()))
                .build();

        try {
            // Flush ngay để bắt lỗi unique trong try/catch,
            // thay vì để lỗi xuất hiện khi transaction commit.
            categoryRepository.saveAndFlush(category);
            return categoryMapper.toResponse(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists: " + normalizedSlug,
                    exception
            );
        }
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found with id : " + id
                ));

        String normalizedSlug = request.getSlug().trim().toLowerCase(Locale.ROOT);
        if (categoryRepository.existsBySlugAndIdNot(normalizedSlug, id)) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists with by slug : " +  normalizedSlug
            );
        }

        category.update(request.getName().trim(), normalizedSlug, normalizeDescription(request.getDescription()));

        if (Boolean.TRUE.equals(request.getActive())) {
            category.setActive(true);
        }
        else {
            category.setActive(false);
        }

        try {
            categoryRepository.saveAndFlush(category);
            return categoryMapper.toResponse(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                    "Category slug already exists with by slug : " +  normalizedSlug,
                    exception
            );
        }
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.deactiveById(id, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(int page, int size) {

        long offset = (page - 1L) * size;

        List<Category> catgories = categoryRepository.getCategories(offset, size);
        List<CategoryResponse> result =
                catgories.stream()
                        .map(categoryMapper::toResponse)
                        .toList();

        long totalElements = categoryRepository.countByActiveTrue();
        int totalPages = (int) Math.ceil(totalElements / (double) size);

        return PageResponse.<CategoryResponse>builder()
                .data(result)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Active category not found with id : " + id
                ));

        return categoryMapper.toResponse(category);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
