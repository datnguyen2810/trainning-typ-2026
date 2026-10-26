package vn.xuandat.backend.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.xuandat.backend.dto.response.ApiResponse;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.dto.response.PageResponse;
import vn.xuandat.backend.service.CategoryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
                    @RequestParam(defaultValue = "1")
                    @Min(value = 1, message = "Page must be at least 1")
                    int page,

                    @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "Size must be at least 1")
                    @Max(value = 100, message = "size must not exceed 100")
                    int size
                ) {
        PageResponse<CategoryResponse> categories = categoryService.getCategories(page, size);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Categories retrieved successfully", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "Category retrieved successfully", category));
    }



}
