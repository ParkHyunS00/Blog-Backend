package com.parkhyuns00.blog.domain.post.controller.dto;

import com.parkhyuns00.blog.domain.post.model.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostCreateRequest(
    @NotBlank
    String title,

    @NotBlank
    String summary,

    @NotBlank
    String content,

    @NotNull
    PostStatus status,

    @NotBlank
    String categoryName,

    List<String> tagNames,

    @NotNull
    Long thumbnailImageId,

    List<Long> contentImageIds
) {
}
