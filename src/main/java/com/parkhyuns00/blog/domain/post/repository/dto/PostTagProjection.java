package com.parkhyuns00.blog.domain.post.repository.dto;

public record PostTagProjection(
    Long postId,
    Long tagId,
    String name,
    String slug
) {
}
