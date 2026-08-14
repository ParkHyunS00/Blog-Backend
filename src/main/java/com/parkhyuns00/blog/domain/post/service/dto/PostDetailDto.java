package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailDto(
    Long postId,
    String title,
    String summary,
    String content,
    Long thumbnailImageId,
    String categoryName,
    String categorySlug,
    List<TagDto> tags,
    List<Long> contentImageIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PostDetailDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
        contentImageIds = contentImageIds == null ? List.of() : List.copyOf(contentImageIds);
    }
}
