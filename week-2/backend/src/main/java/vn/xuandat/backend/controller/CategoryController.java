package vn.xuandat.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.xuandat.backend.dto.response.ApiResponse;
import vn.xuandat.backend.dto.response.CategoryResponse;
import vn.xuandat.backend.dto.response.PageResponse;
import vn.xuandat.backend.service.CategoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
                    @RequestParam(defaultValue = "1", required = false) int page,
                    @RequestParam(defaultValue = "10", required = false) int size
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
