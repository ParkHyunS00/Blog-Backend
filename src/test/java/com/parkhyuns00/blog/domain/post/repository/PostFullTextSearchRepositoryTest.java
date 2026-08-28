package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

@Testcontainers
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
@ActiveProfiles("test")
public class PostFullTextSearchRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("게시글 제목과 본문에 Full Text Index 를 생성한다.")
    void test_create_post_fts_success() {
        List<String> indexNames = jdbcTemplate.queryForList("""
              SELECT DISTINCT INDEX_NAME
              FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'posts'
                AND INDEX_TYPE = 'FULLTEXT'
              """, String.class);

        assertThat(indexNames).contains("ft_posts_title", "ft_posts_content");
    }

    @Test
    @DisplayName("검색어가 포함된 공개 게시글을 조회한다.")
    void test_find_published_posts_by_keyword_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post matchedPost = postRepository.save(Post.publish("스프링 보안", "Spring Security 정리", "세션 인증 방식을 설명합니다.", category));

        postRepository.save(Post.publish("리눅스 서버", "Linux Server 정리", "방화벽과 네트워크를 설명합니다.", category));
        postRepository.save(Post.createDraft("스프링 내부 문서", "작성 중인 게시글", "아직 공개하지 않은 내용입니다.", category));

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), "스프링");

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 5));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(matchedPost.getId());
    }

    @Test
    @DisplayName("제목에서 검색어가 일치한 게시글을 본문에서 일치한 게시글보다 먼저 조회한다.")
    void test_find_published_posts_by_keyword_order_by_relevance_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post contentMatchedPost = postRepository.save(Post.publish("인증 방식", "본문 검색 결과", "스프링 보안을 적용하는 방법을 설명합니다.", category));
        Post titleMatchedPost = postRepository.save(Post.publish("스프링 보안 설정", "제목 검색 결과", "세션 인증 방법을 설명합니다.", category));

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), "스프링 보안");

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 5));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(
                titleMatchedPost.getId(),
                contentMatchedPost.getId()
            );
    }

    @Test
    @DisplayName("검색 결과를 페이지 단위로 조회한다.")
    void test_find_published_posts_by_keyword_with_pagination_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        for (int index = 1; index <= 6; index++) {
            postRepository.save(
                Post.publish(
                    "스프링 게시글 " + index,
                    "summary " + index,
                    "스프링 관련 내용 " + index,
                    category
                )
            );
        }

        postRepository.save(Post.publish("리눅스 서버", "Linux Server", "네트워크와 방화벽에 관한 내용입니다.", category));

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), "스프링");

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(1, 5));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("검색어와 일치하는 공개 게시글이 없으면 빈 페이지를 반환한다.")
    void test_find_published_posts_by_keyword_empty() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        postRepository.save(
            Post.publish(
                "리눅스 서버",
                "Linux Server",
                "네트워크와 방화벽을 설명합니다.",
                category
            )
        );

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), "스프링");

        Page<PostSummaryDto> result =
            postRepository.findPublishedPosts(condition, PageRequest.of(0, 5));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }
}
