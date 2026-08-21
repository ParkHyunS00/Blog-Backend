package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDraftDetailDto(
    Long postId,
    String title,
    String summary,
    String content,
    CategoryDto category,
    List<TagDto> tags,
    Long thumbnailImageId,
    List<Long> contentImageIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PostDraftDetailDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
        contentImageIds = contentImageIds == null ? List.of() : List.copyOf(contentImageIds);
    }
}
