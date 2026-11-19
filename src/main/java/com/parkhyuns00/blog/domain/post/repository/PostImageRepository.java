package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostId(Long postId);

    Optional<PostImage> findByIdAndPostStatus(Long id, PostStatus status);
}
