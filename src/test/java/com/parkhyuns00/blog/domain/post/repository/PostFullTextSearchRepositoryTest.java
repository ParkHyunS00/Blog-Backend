package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostTag;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.repository.TagRepository;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Testcontainers
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.parkhyuns00.blog.domain.post.repository.PostFullTextSearchRepositoryTest$SqlCapture"
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

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private TagRepository tagRepository;

    public static class SqlCapture implements StatementInspector {
        private static final ThreadLocal<List<String>> STATEMENTS = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public String inspect(String sql) {
            STATEMENTS.get().add(sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim());
            return sql;
        }
    }

    @BeforeEach
    void setUp() {
        postRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        SqlCapture.STATEMENTS.get().clear();
    }

    @AfterEach
    void clearSqlCapture() {
        SqlCapture.STATEMENTS.remove();
    }

    @Test
    @DisplayName("발행된 제목과 본문에 하나의 복합 Full Text Index를 생성한다.")
    void test_create_post_fts_success() {
        List<String> indexNames = jdbcTemplate.queryForList("""
              SELECT DISTINCT INDEX_NAME
              FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'posts'
                AND INDEX_TYPE = 'FULLTEXT'
              """, String.class);

        assertThat(indexNames).containsExactly("ft_posts_title_content");

        List<String> columns = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'posts'
              AND INDEX_NAME = 'ft_posts_title_content'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertThat(columns).containsExactly("published_title", "published_content");
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
    @DisplayName("제목과 본문의 복합 검색 관련도 순서로 게시글을 조회한다.")
    void test_find_published_posts_by_keyword_order_by_relevance_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post contentMatchedPost = postRepository.save(Post.publish("인증 방식", "본문 검색 결과", "스프링 ".repeat(8), category));
        Post titleMatchedPost = postRepository.save(Post.publish("스프링 설정", "제목 검색 결과", "세션 인증 방법을 설명합니다.", category));
        postRepository.save(Post.publish("리눅스 서버", "서버 설정", "네트워크와 방화벽을 설명합니다.", category));

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), "스프링");

        Map<Long, Double> relevanceByPostId = jdbcTemplate.query("""
            SELECT id, MATCH(published_title, published_content)
                AGAINST (? IN NATURAL LANGUAGE MODE) AS relevance
            FROM posts
            """, (row, _) -> Map.entry(row.getLong("id"), row.getDouble("relevance")), "스프링")
            .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 5));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactlyInAnyOrder(
                titleMatchedPost.getId(),
                contentMatchedPost.getId()
            );
        assertThat(relevanceByPostId.get(contentMatchedPost.getId()))
            .isGreaterThan(relevanceByPostId.get(titleMatchedPost.getId()));
        assertThat(result.getContent())
            .extracting(summary -> relevanceByPostId.get(summary.postId()))
            .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("검색 결과와 메타데이터는 지연 조인하고 태그까지 세 번의 쿼리로 조회한다.")
    void test_find_published_posts_with_metadata_and_tags_in_three_queries() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post post = postRepository.save(Post.publish("스프링 검색", "검색 요약", "게시글 검색을 설명합니다.", category));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        postTagRepository.save(new PostTag(post, spring));
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "search/thumbnail.webp", "image/webp", 1200, 630);
        thumbnail.attachTo(post);
        postImageRepository.save(thumbnail);
        SqlCapture.STATEMENTS.get().clear();

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            new PostSearchCondition(null, List.of(), "스프링"), PageRequest.of(0, 5)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        PostSummaryDto summary = result.getContent().getFirst();
        assertThat(summary.postId()).isEqualTo(post.getId());
        assertThat(summary.title()).isEqualTo("스프링 검색");
        assertThat(summary.summary()).isEqualTo("검색 요약");
        assertThat(summary.thumbnailImageId()).isEqualTo(thumbnail.getId());
        assertThat(summary.categoryName()).isEqualTo("Backend");
        assertThat(summary.categorySlug()).isEqualTo("backend");
        assertThat(summary.createdAt()).isNotNull();
        assertThat(summary.updatedAt()).isNotNull();
        assertThat(summary.tags()).extracting(TagDto::slug).containsExactly("spring");

        List<String> statements = SqlCapture.STATEMENTS.get();
        assertThat(statements).hasSize(3);
        assertThat(statements.getFirst())
            .contains("from (", "published_title", "published_content", "limit ?, ?", "join posts", "join categories", "left join post_images")
            .doesNotContain("created_at desc", "id desc");
        assertThat(statements.get(1)).contains("count(*)", "match(").doesNotContain("status");
        assertThat(statements.get(2)).contains("post_tags", "join tags");
    }

    @Test
    @DisplayName("초안을 발행하면 검색되고 발행글 수정은 새로운 제목과 본문 검색에 반영된다.")
    void test_search_reflects_publishing_and_published_content_update() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post draft = postRepository.save(Post.createDraft("스프링 초안", "요약", "스프링 내용을 설명합니다.", category));
        postRepository.save(Post.publish("데이터베이스", "다른 게시글", "트랜잭션을 설명합니다.", category));
        PostSearchCondition springCondition = new PostSearchCondition(null, List.of(), "스프링");

        assertThat(postRepository.findPublishedPosts(springCondition, PageRequest.of(0, 5)).getTotalElements()).isZero();

        draft.publish();
        Post published = postRepository.saveAndFlush(draft);

        assertThat(postRepository.findPublishedPosts(springCondition, PageRequest.of(0, 5)).getContent())
            .extracting(PostSummaryDto::postId).containsExactly(draft.getId());

        published.updatePublished("리눅스 서버", "수정 요약", "네트워크 설정을 설명합니다.", category);
        postRepository.saveAndFlush(published);

        assertThat(postRepository.findPublishedPosts(springCondition, PageRequest.of(0, 5)).getTotalElements()).isZero();
        assertThat(postRepository.findPublishedPosts(
            new PostSearchCondition(null, List.of(), "리눅스"), PageRequest.of(0, 5)
        ).getContent()).extracting(PostSummaryDto::title).containsExactly("리눅스 서버");
        assertThat(postRepository.findPublishedPosts(
            new PostSearchCondition(null, List.of(), "네트워크"), PageRequest.of(0, 5)
        ).getContent()).extracting(PostSummaryDto::postId).containsExactly(published.getId());
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
        SqlCapture.STATEMENTS.get().clear();

        Page<PostSummaryDto> result =
            postRepository.findPublishedPosts(condition, PageRequest.of(0, 5));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(SqlCapture.STATEMENTS.get()).hasSize(2);
    }

    @Test
    @DisplayName("검색 페이지 범위를 벗어나도 전체 개수를 반환하고 태그 조회는 생략한다.")
    void test_find_published_posts_by_keyword_with_out_of_range_page() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        postRepository.save(Post.publish("스프링 검색", "요약", "검색을 설명합니다.", category));
        SqlCapture.STATEMENTS.get().clear();

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            new PostSearchCondition(null, List.of(), "스프링"), PageRequest.of(10, 5)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(SqlCapture.STATEMENTS.get()).hasSize(2);
    }
}
