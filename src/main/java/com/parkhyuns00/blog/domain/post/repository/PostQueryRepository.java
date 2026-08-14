package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostQueryRepository {
    Page<PostSummaryDto> findPublishedPosts(PostSearchCondition condition, Pageable pageable);
    Optional<PostDetailDto> findPublishedPostById(Long postId);
}
