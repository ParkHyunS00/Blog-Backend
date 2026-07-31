package com.parkhyuns00.blog.domain.category.model;

import com.parkhyuns00.blog.domain.category.exception.CategoryException;
import com.parkhyuns00.blog.domain.category.exception.CategoryExceptionCode;
import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
public class Category extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_SLUG_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    public Category(String name, String slug) {
        validate(name, slug);
        this.name = name.trim();
        this.slug = slug;
    }

    private void validate(String name, String slug) {
        if (name == null || name.isBlank()) {
            throw new CategoryException(CategoryExceptionCode.INVALID_CATEGORY_NAME);
        }

        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new CategoryException(CategoryExceptionCode.INVALID_CATEGORY_NAME);
        }

        if (slug == null || slug.isBlank()) {
            throw new CategoryException(CategoryExceptionCode.INVALID_CATEGORY_NAME);
        }

        if (slug.length() > MAX_SLUG_LENGTH) {
            throw new CategoryException(CategoryExceptionCode.INVALID_CATEGORY_NAME);
        }
    }
}
