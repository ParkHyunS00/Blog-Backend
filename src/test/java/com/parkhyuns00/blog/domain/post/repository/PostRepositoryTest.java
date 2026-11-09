package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.service.dto.PostPopularDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
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
    @DisplayName("게시글 상태와 관계없이 조회수가 높은 게시글을 최대 3개 반환한다.")
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

        List<PostPopularDto> result = postRepository.findPopularPosts(3);

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(draft.getId(), second.getId(), fourth.getId());

        assertThat(result)
            .extracting(PostPopularDto::viewCount)
            .containsExactly(100L, 40L, 30L);

        PostPopularDto popularPost = result.getFirst();

        assertThat(popularPost.title()).isEqualTo("임시저장");
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

        List<PostPopularDto> result = postRepository.findPopularPosts(3);

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(second.getId(), first.getId(), third.getId());

        assertThat(result)
            .extracting(PostPopularDto::createdAt)
            .containsExactly(recent, recent, recent.minusDays(1));
    }

    @Test
    @DisplayName("조회수가 양수인 게시글이 3개 미만이면 조회수 0인 글로 채우지 않는다.")
    void test_find_popular_posts_success_when_less_than_three() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post first = Post.publish("첫 번째", "요약", "본문", category);
        Post second = Post.publish("두 번째", "요약", "본문", category);

        ReflectionTestUtils.setField(first, "viewCount", 20L);
        ReflectionTestUtils.setField(second, "viewCount", 10L);

        postRepository.saveAllAndFlush(List.of(
            first,
            second,
            Post.publish("조회수 없는 글", "요약", "본문", category)
        ));

        List<PostPopularDto> result = postRepository.findPopularPosts(3);

        assertThat(result)
            .extracting(PostPopularDto::postId)
            .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("조회수가 양수인 게시글이 없으면 빈 목록을 반환한다.")
    void test_find_popular_posts_success_when_no_viewed_posts() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        postRepository.saveAllAndFlush(List.of(
            Post.createDraft("임시저장", "요약", "본문", category),
            Post.publish("공개 글", "요약", "본문", category)
        ));

        List<PostPopularDto> result = postRepository.findPopularPosts(3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상위 3개 ID를 먼저 선택한 후 카테고리가 없는 글은 조인에서 제외한다.")
    void test_find_popular_posts_limits_before_category_join() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post uncategorized = Post.createDraft("카테고리 없는 글", "요약", "본문", null);
        Post first = Post.publish("첫 번째", "요약", "본문", category);
        Post second = Post.publish("두 번째", "요약", "본문", category);
        Post third = Post.publish("세 번째", "요약", "본문", category);

        ReflectionTestUtils.setField(uncategorized, "viewCount", 100L);
        ReflectionTestUtils.setField(first, "viewCount", 30L);
        ReflectionTestUtils.setField(second, "viewCount", 20L);
        ReflectionTestUtils.setField(third, "viewCount", 10L);
        postRepository.saveAllAndFlush(List.of(uncategorized, first, second, third));

        assertThat(postRepository.findPopularPosts(3))
            .extracting(PostPopularDto::postId)
            .containsExactly(first.getId(), second.getId());
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
