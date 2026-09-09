package com.parkhyuns00.blog.domain.category.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryWithLatestPostDto;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

@ActiveProfiles("test")
@DataJpaTest
@Import({
    QueryDslConfig.class,
    JpaConfig.class
})
public class CategoryQueryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("카테고리별 공개 게시글 개수와 최신글을 이름순으로 반환한다.")
    void test_find_all_with_latest_post_success() {
        Category database = categoryRepository.save(new Category("Database", "database"));
        Category backend = categoryRepository.save(new Category("Backend", "backend"));

        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);

        Post latestBackendPost = savePost(
            Post.publish("최신 제목", "최신 요약", "본문", backend),
            base.plusDays(1)
        );

        // 더 큰 ID가 있어도 작성일이 오래되면 최신글이 아님
        savePost(
            Post.publish("이전 제목", "이전 요약", "본문", backend),
            base
        );

        Post databasePost = savePost(
            Post.publish("DB 제목", "DB 요약", "본문", database),
            base
        );

        List<CategoryWithLatestPostDto> result = categoryRepository.findAllWithLatestPost(PostStatus.PUBLISHED);

        assertThat(result)
            .extracting(CategoryWithLatestPostDto::categoryName)
            .containsExactly("Backend", "Database");

        CategoryWithLatestPostDto backendResult = result.getFirst();

        assertThat(backendResult.categoryId()).isEqualTo(backend.getId());
        assertThat(backendResult.categorySlug()).isEqualTo("backend");
        assertThat(backendResult.postCount()).isEqualTo(2L);
        assertThat(backendResult.latestPost().postId()).isEqualTo(latestBackendPost.getId());
        assertThat(backendResult.latestPost().title()).isEqualTo("최신 제목");
        assertThat(backendResult.latestPost().summary()).isEqualTo("최신 요약");
        assertThat(backendResult.latestPost().createdAt()).isEqualTo(base.plusDays(1));

        CategoryWithLatestPostDto databaseResult = result.get(1);

        assertThat(databaseResult.postCount()).isEqualTo(1L);
        assertThat(databaseResult.latestPost().postId()).isEqualTo(databasePost.getId());
    }

    @Test
    @DisplayName("더 최근에 작성한 임시저장 글은 개수와 최신글 선정에서 제외한다.")
    void test_find_all_with_latest_post_exclude_draft() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);

        Post publishedPost = savePost(
            Post.publish("공개 제목", "공개 요약", "본문", category),
            base
        );
        savePost(
            Post.createDraft("임시 제목", "임시 요약", "본문", category),
            base.plusDays(1)
        );

        List<CategoryWithLatestPostDto> result =
            categoryRepository.findAllWithLatestPost(PostStatus.PUBLISHED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().postCount()).isEqualTo(1L);
        assertThat(result.getFirst().latestPost().postId())
            .isEqualTo(publishedPost.getId());
    }

    @Test
    @DisplayName("공개 게시글이 없는 카테고리는 제외한다.")
    void test_find_all_with_latest_post_exclude_categories_without_published_posts() {
        categoryRepository.save(new Category("Empty", "empty"));
        Category draftCategory = categoryRepository.save(new Category("Draft", "draft"));

        savePost(
            Post.createDraft("임시 제목", "임시 요약", "본문", draftCategory),
            LocalDateTime.of(2026, 9, 1, 12, 0)
        );

        List<CategoryWithLatestPostDto> result = categoryRepository.findAllWithLatestPost(PostStatus.PUBLISHED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("작성일이 같으면 ID가 큰 공개 게시글을 최신글로 반환한다.")
    void test_find_all_with_latest_post_success_when_created_at_equal() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 12, 0);

        Post firstPost = savePost(
            Post.publish("첫 제목", "첫 요약", "본문", category),
            createdAt
        );
        Post secondPost = savePost(
            Post.publish("두 번째 제목", "두 번째 요약", "본문", category),
            createdAt
        );

        List<CategoryWithLatestPostDto> result = categoryRepository.findAllWithLatestPost(PostStatus.PUBLISHED);

        assertThat(secondPost.getId()).isGreaterThan(firstPost.getId());
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().postCount()).isEqualTo(2L);
        assertThat(result.getFirst().latestPost().postId()).isEqualTo(secondPost.getId());
    }

    @Test
    @DisplayName("등록된 카테고리가 없으면 빈 목록을 반환한다.")
    void test_find_all_with_latest_post_success_when_categories_empty() {
        List<CategoryWithLatestPostDto> result = categoryRepository.findAllWithLatestPost(PostStatus.PUBLISHED);

        assertThat(result).isEmpty();
    }

    private Post savePost(Post post, LocalDateTime createdAt) {
        Post savedPost = postRepository.saveAndFlush(post);

        entityManager.createQuery("update Post p set p.createdAt = :createdAt where p.id = :postId")
            .setParameter("createdAt", createdAt)
            .setParameter("postId", savedPost.getId())
            .executeUpdate();

        entityManager.refresh(savedPost);

        return savedPost;
    }
}