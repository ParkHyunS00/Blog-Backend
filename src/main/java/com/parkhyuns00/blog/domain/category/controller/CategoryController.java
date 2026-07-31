package com.parkhyuns00.blog.domain.category.controller;

import com.parkhyuns00.blog.domain.category.repository.dto.CategoryWithPostCountDto;
import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.global.response.StandardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    public ResponseEntity<StandardResponse<List<CategoryWithPostCountDto>>> getCategoriesWithPostCount() {
        return StandardResponse.ok(categoryService.getCategoriesWithPostCount());
    }
}
