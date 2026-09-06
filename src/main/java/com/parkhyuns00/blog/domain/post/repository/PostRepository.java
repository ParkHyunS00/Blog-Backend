package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.dto.PostPopularDto;
import org.springframework.data.domain.Limit;
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

    @Query("""
        select new com.parkhyuns00.blog.domain.post.service.dto.PostPopularDto(
            p.id,
            p.title,
            c.name,
            c.slug,
            p.createdAt,
            p.viewCount
        )
        from Post p
        join p.category c
        where p.status = :status
        order by p.viewCount desc, p.createdAt desc, p.id desc
    """)
    List<PostPopularDto> findPopularPosts(@Param("status") PostStatus status, Limit limit);
}
