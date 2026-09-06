package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.dto.PostPopularDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

@ActiveProfiles("test")
@DataJpaTest
@Import({
    QueryDslConfig.class,
    JpaConfig.class
})
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("게시글의 조회수를 원자적으로 1 증가시킨다.")
    void test_increment_published_post_view_count_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post post = postRepository.save(Post.publish("title", "summary", "content", category));

        int affectedRows = postRepository.incrementViewCount(post.getId());

        entityManager.flush();
        entityManager.clear();

        Post foundPost = postRepository.findById(post.getId()).orElseThrow();

        assertThat(affectedRows).isEqualTo(1);
        assertThat(foundPost.getViewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("조회수가 높은 공개 게시글을 최대 3개 반환한다.")
    void test_find_popular_posts_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post first = Post.publish("첫번째", "요약", "본문", category);
        Post second = Post.publish("두번째", "요약", "본문", category);
        Post third = Post.publish("세번째", "요약", "본문", category);
        Post fourth = Post.publish("네번째", "요약", "본문", category);
        Post draft = Post.createDraft("임시저장", "요약", "본문", category);

        ReflectionTestUtils.setField(first, "viewCount", 10L);
        ReflectionTestUtils.setField(second, "viewCount", 40L);
        ReflectionTestUtils.setField(third, "viewCount", 20L);
        ReflectionTestUtils.setField(fourth, "viewCount", 30L);
        ReflectionTestUtils.setField(draft, "viewCount", 100L);

        postRepository.saveAllAndFlush(List.of(first, second, third, fourth, draft));

        List<PostPopularDto> result = postRepository.findPopularPosts(PostStatus.PUBLISHED, Limit.of(3));

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(second.getId(), fourth.getId(), third.getId());

        assertThat(result)
            .extracting(PostPopularDto::viewCount)
            .containsExactly(40L, 30L, 20L);

        PostPopularDto popularPost = result.getFirst();

        assertThat(popularPost.title()).isEqualTo("두번째");
        assertThat(popularPost.categoryName()).isEqualTo("Backend");
        assertThat(popularPost.categorySlug()).isEqualTo("backend");
        assertThat(popularPost.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("조회수가 같으면 작성일과 ID 내림차순으로 반환한다.")
    void test_find_popular_posts_success_when_view_count_equal() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post first = Post.publish("첫 번째", "요약", "본문", category);
        Post second = Post.publish("두 번째", "요약", "본문", category);
        Post third = Post.publish("세 번째", "요약", "본문", category);

        ReflectionTestUtils.setField(first, "viewCount", 10L);
        ReflectionTestUtils.setField(second, "viewCount", 10L);
        ReflectionTestUtils.setField(third, "viewCount", 10L);

        postRepository.saveAllAndFlush(List.of(first, second, third));

        LocalDateTime recent = LocalDateTime.of(2026, 9, 6, 12, 0);

        updateCreatedAt(first.getId(), recent);
        updateCreatedAt(second.getId(), recent);

        // ID가 가장 커도 작성일이 오래되면 뒤에 위치
        updateCreatedAt(third.getId(), recent.minusDays(1));

        entityManager.clear();

        List<PostPopularDto> result = postRepository.findPopularPosts(PostStatus.PUBLISHED, Limit.of(3));

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(second.getId(), first.getId(), third.getId());

        assertThat(result)
            .extracting(PostPopularDto::createdAt)
            .containsExactly(recent, recent, recent.minusDays(1));
    }

    @Test
    @DisplayName("공개 게시글이 3개 미만이면 존재하는 만큼 반환한다.")
    void test_find_popular_posts_success_when_less_than_three() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post first = Post.publish("첫 번째", "요약", "본문", category);
        Post second = Post.publish("두 번째", "요약", "본문", category);

        ReflectionTestUtils.setField(first, "viewCount", 20L);
        ReflectionTestUtils.setField(second, "viewCount", 10L);

        postRepository.saveAllAndFlush(List.of(first, second));

        List<PostPopularDto> result = postRepository.findPopularPosts(PostStatus.PUBLISHED, Limit.of(3));

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("공개 게시글이 없으면 빈 목록을 반환한다.")
    void test_find_popular_posts_success_when_no_published_posts() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        postRepository.saveAndFlush(Post.createDraft("임시저장", "요약", "본문", category));

        List<PostPopularDto> result = postRepository.findPopularPosts(PostStatus.PUBLISHED, Limit.of(3));

        assertThat(result).isEmpty();
    }

    private void updateCreatedAt(Long postId, LocalDateTime createdAt) {
        entityManager.createQuery("""
              update Post p
              set p.createdAt = :createdAt
              where p.id = :postId
              """)
            .setParameter("createdAt", createdAt)
            .setParameter("postId", postId)
            .executeUpdate();
    }
}
