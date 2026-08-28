package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryDto(
    Long postId,
    String title,
    String summary,
    Long thumbnailImageId,
    String categoryName,
    String categorySlug,
    List<TagDto> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PostSummaryDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
