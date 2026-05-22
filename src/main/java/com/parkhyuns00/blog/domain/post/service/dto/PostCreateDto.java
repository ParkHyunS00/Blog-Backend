package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;

public record PostCreateDto(
    Long postId,
    PostStatus status
) {
    public static PostCreateDto from(Post post) {
        return new PostCreateDto(post.getId(), post.getStatus());
    }
}
