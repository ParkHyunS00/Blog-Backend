package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.dto.PostPopularDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostQueryRepository {

    Optional<Post> findByIdAndStatus(Long postId, PostStatus status);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Query(value = """
        SELECT
            p.id,
            p.title,
            c.name,
            c.slug,
            p.created_at,
            p.view_count
        FROM (
            SELECT p2.id
            FROM posts p2
            WHERE p2.view_count > 0
            ORDER BY p2.view_count DESC, p2.created_at DESC, p2.id DESC
            LIMIT :limit
        ) top_posts
        JOIN posts p ON p.id = top_posts.id
        JOIN categories c ON c.id = p.category_id
        ORDER BY p.view_count DESC, p.created_at DESC, p.id DESC
        """, nativeQuery = true)
    List<PostPopularDto> findPopularPosts(@Param("limit") int limit);
}
