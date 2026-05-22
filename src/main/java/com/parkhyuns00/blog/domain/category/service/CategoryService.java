package com.parkhyuns00.blog.domain.category.service;

import com.parkhyuns00.blog.domain.category.exception.CategoryException;
import com.parkhyuns00.blog.domain.category.exception.CategoryExceptionCode;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.category.repository.dto.CategoryWithPostCountDto;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugUtil slugUtil;

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
            .map(CategoryDto::from)
            .toList();
    }

    public List<CategoryWithPostCountDto> getCategoriesWithPostCount() {
        return categoryRepository.findAllWithPostCount(PostStatus.PUBLISHED);
    }

    @Transactional
    public Category getOrCreateByName(String name) {
        String normalizedName = name.trim();
        String slug = generateSlug(normalizedName);

        return categoryRepository.findBySlug(slug).orElseGet(() -> create(normalizedName, slug));
    }

    private Category create(String name, String slug) {
        if (categoryRepository.existsByName(name)) {
            throw new CategoryException(CategoryExceptionCode.CATEGORY_NAME_DUPLICATED);
        }

        try {
            return categoryRepository.saveAndFlush(new Category(name, slug));
        } catch (DataAccessException exception) {
            throw new CategoryException(CategoryExceptionCode.CATEGORY_SAVE_FAILED, exception);
        }
    }

    private String generateSlug(String name) {
        try {
            return slugUtil.generate(name);
        } catch (IllegalArgumentException exception) {
            throw new CategoryException(CategoryExceptionCode.INVALID_CATEGORY_NAME, exception);
        }
    }
}
