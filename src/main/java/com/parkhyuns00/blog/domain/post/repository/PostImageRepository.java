package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostId(Long postId);
}
