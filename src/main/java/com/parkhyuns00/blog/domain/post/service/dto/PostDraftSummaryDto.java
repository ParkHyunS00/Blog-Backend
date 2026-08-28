package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDraftSummaryDto(
    Long postId,
    String title,
    CategoryDto category,
    List<TagDto> tags,
    LocalDateTime updatedAt
) {
    public PostDraftSummaryDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
