package com.parkhyuns00.blog.domain.post.controller.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record PostDraftUpdateRequest(
    @Size(max = 200)
    String title,

    @Size(max = 500)
    String summary,

    String content,
    String categoryName,
    List<String> tagNames,
    Long thumbnailImageId,
    List<Long> contentImageIds
) {
}
