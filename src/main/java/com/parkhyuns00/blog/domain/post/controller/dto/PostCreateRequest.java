package com.parkhyuns00.blog.domain.post.controller.dto;

import com.parkhyuns00.blog.domain.post.model.PostStatus;

import java.util.List;

public record PostCreateRequest(
    String title,
    String summary,
    String content,
    PostStatus status,
    String categoryName,
    List<String> tagNames,
    Long thumbnailImageId,
    List<Long> contentImageIds
) {
}
