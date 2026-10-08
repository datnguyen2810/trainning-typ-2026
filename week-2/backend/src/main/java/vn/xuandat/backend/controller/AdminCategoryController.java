package vn.xuandat.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.xuandat.backend.dto.request.CreateCategoryRequest;
import vn.xuandat.backend.dto.request.UpdateCategoryRequest;
import vn.xuandat.backend.dto.response.ApiResponse;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.service.CategoryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse createdCategory = categoryService.createCategory(request);

        ApiResponse<CategoryResponse> response = ApiResponse.of(HttpStatus.CREATED, "Category created successfully", createdCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);

        ApiResponse<CategoryResponse> response = ApiResponse.of(HttpStatus.OK, "Category updated successfully", updatedCategory);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);

        ApiResponse<Void> response = ApiResponse.message(HttpStatus.OK, "Category deleted successfully");
        return ResponseEntity.ok(response);
    }
}

