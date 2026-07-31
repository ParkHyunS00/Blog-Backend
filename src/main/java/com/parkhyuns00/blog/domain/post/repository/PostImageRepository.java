package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
