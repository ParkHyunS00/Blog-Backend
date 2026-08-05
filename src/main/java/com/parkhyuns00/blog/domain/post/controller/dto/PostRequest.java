package com.parkhyuns00.blog.domain.post.controller.dto;

import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;

import java.util.List;

public record PostRequest(
    String category,
    List<String> tags,
    String keyword
) {
    public PostSearchCondition toCondition() {
        return new PostSearchCondition(
            normalize(category),
            normalizeTags(tags),
            normalize(keyword)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        return tags.stream()
            .filter(tag -> tag != null && !tag.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }
}
