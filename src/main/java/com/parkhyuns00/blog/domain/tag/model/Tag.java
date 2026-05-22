package com.parkhyuns00.blog.domain.tag.model;

import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import com.parkhyuns00.blog.domain.tag.exception.TagException;
import com.parkhyuns00.blog.domain.tag.exception.TagExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tags")
public class Tag extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 20;
    private static final int MAX_SLUG_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    public Tag(String name, String slug) {
        validate(name, slug);
        this.name = name.trim();
        this.slug = slug;
    }

    private void validate(String name, String slug) {
        if (name == null || name.isBlank()) {
            throw new TagException(TagExceptionCode.INVALID_TAG_NAME);
        }

        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new TagException(TagExceptionCode.INVALID_TAG_NAME);
        }

        if (slug == null || slug.isBlank()) {
            throw new TagException(TagExceptionCode.INVALID_TAG_NAME);
        }

        if (slug.length() > MAX_SLUG_LENGTH) {
            throw new TagException(TagExceptionCode.INVALID_TAG_NAME);
        }
    }
}
