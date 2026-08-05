package com.parkhyuns00.blog.domain.post.controller.dto;

import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record PostRequest(
    String category,
    List<String> tags,
    String keyword,

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 5, message = "페이지 크기는 5 이하여야 합니다.")
    Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;

    public Pageable toPageable() {
        return PageRequest.of(
            page == null ? DEFAULT_PAGE : page,
            size == null ? DEFAULT_SIZE : size
        );
    }

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
