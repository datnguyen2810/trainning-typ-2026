package vn.xuandat.backend.service;

import vn.xuandat.backend.dto.request.CreateCategoryRequest;
import vn.xuandat.backend.dto.request.UpdateCategoryRequest;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.dto.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);

    PageResponse<CategoryResponse> getCategories(int page, int size);

    CategoryResponse getCategoryById(UUID id);
}

