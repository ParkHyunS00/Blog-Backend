package com.parkhyuns00.blog.domain.post.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
    @NotBlank
    @Size(max = 200)
    String title,

    @NotBlank
    @Size(max = 500)
    String summary,

    @NotBlank
    String content,

    @NotBlank
    String categoryName,

    List<String> tagNames,

    Long thumbnailImageId,

    List<Long> contentImageIds
) {
}
