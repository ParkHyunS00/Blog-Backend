package com.parkhyuns00.blog.domain.post.service.dto;

import java.util.List;

public record PostSearchCondition(
    String categorySlug,
    List<String> tagSlugs,
    String keyword
) {
    public PostSearchCondition {
        tagSlugs = tagSlugs == null
            ? List.of()
            : List.copyOf(tagSlugs);
    }

    public boolean hasCategory() {
        return categorySlug != null && !categorySlug.isBlank();
    }

    public boolean hasTags() {
        return !tagSlugs.isEmpty();
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
