package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostQueryRepository {

    Page<PostSummaryDto> findPublishedPosts(PostSearchCondition condition, Pageable pageable);
    Optional<PostDetailDto> findPublishedPostById(Long postId);
    Page<PostDraftSummaryDto> findDraftPosts(Pageable pageable);
    Optional<PostDraftDetailDto> findDraftPostById(Long postId);
}
