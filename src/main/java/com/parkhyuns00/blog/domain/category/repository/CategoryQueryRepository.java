package com.parkhyuns00.blog.domain.category.repository;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryWithLatestPostDto;
import com.parkhyuns00.blog.domain.post.model.PostStatus;

import java.util.List;

public interface CategoryQueryRepository {

    List<CategoryWithLatestPostDto> findAllWithLatestPost(PostStatus status);
}
