package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryRepository {
    Page<PostSummaryDto> findPublishedPosts(PostSearchCondition condition, Pageable pageable);
}
