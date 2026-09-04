package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostQueryRepository {

    Optional<Post> findByIdAndStatus(Long postId, PostStatus status);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

}
