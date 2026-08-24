package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostQueryRepository {

    Optional<Post> findByIdAndStatus(Long postId, PostStatus status);
}
