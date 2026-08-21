package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostTag;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDraftSummaryDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.repository.TagRepository;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

@ActiveProfiles("test")
@DataJpaTest
@Import({
    QueryDslConfig.class,
    JpaConfig.class
})
public class PostQueryRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Test
    @DisplayName("공개 게시글만 최신순으로 조회한다.")
    void test_find_published_posts_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post oldPost = postRepository.save(
            Post.publish("old", "old summary", "old content", category)
        );
        Post draftPost = postRepository.save(
            Post.createDraft("draft", "draft summary", "draft content", category)
        );
        Post newPost = postRepository.save(
            Post.publish("new", "new summary", "new content", category)
        );

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), null);
        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            condition,
            PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(PostSummaryDto::postId).containsExactly(newPost.getId(), oldPost.getId());
        assertThat(result.getContent()).extracting(PostSummaryDto::postId).doesNotContain(draftPost.getId());
    }

    @Test
    @DisplayName("게시글 목록을 조회하면 게시글과 카테고리 및 썸네일 정보를 반환한다.")
    void test_find_published_posts_with_summary_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post post = postRepository.save(
            Post.publish("title", "summary", "content", category)
        );
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "thumbnail/object-key", "image/webp");

        thumbnail.attachTo(post);
        postImageRepository.save(thumbnail);

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            condition,
            PageRequest.of(0, 10)
        );

        PostSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.postId()).isEqualTo(post.getId());
        assertThat(summary.title()).isEqualTo("title");
        assertThat(summary.summary()).isEqualTo("summary");
        assertThat(summary.thumbnailImageId()).isEqualTo(thumbnail.getId());
        assertThat(summary.categoryName()).isEqualTo("Backend");
        assertThat(summary.categorySlug()).isEqualTo("backend");
        assertThat(summary.createdAt()).isNotNull();
        assertThat(summary.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 목록을 조회하면 게시글의 태그 목록을 반환한다.")
    void test_find_published_posts_with_tags_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        Post post = postRepository.save(Post.publish("title", "summary", "content", category));

        postTagRepository.saveAll(List.of(new PostTag(post, spring), new PostTag(post, java)));

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 10));

        PostSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.tags())
            .extracting(TagDto::slug)
            .containsExactly("java", "spring");
    }

    @Test
    @DisplayName("게시글에 태그가 없으면 빈 태그 목록을 반환한다.")
    void test_find_published_posts_without_tags_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post post = postRepository.save(Post.publish("title", "summary", "content", category));

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            new PostSearchCondition(null, List.of(), null),
            PageRequest.of(0, 10)
        );

        PostSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.postId()).isEqualTo(post.getId());
        assertThat(summary.tags()).isEmpty();
    }

    @Test
    @DisplayName("게시글 목록을 페이지 단위로 조회한다.")
    void test_find_published_posts_with_pagination_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        for (int index = 1; index <= 5; index++) {
            postRepository.save(Post.publish("title " + index, "summary " + index, "content " + index, category));
        }

        PostSearchCondition condition = new PostSearchCondition(null, List.of(), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            condition,
            PageRequest.of(1, 2)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("카테고리에 해당하는 공개 게시글을 조회한다.")
    void test_find_published_posts_by_category_success() {
        Category backend = categoryRepository.save(new Category("Backend", "backend"));
        Category frontend = categoryRepository.save(new Category("Frontend", "frontend"));
        Post backendPost = postRepository.save(Post.publish("backend post", "backend summary", "backend content", backend));

        postRepository.save(Post.publish("frontend post", "frontend summary", "frontend content", frontend));

        postRepository.save(Post.createDraft("backend draft", "draft summary", "draft content", backend));

        PostSearchCondition condition = new PostSearchCondition("backend", List.of(), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(backendPost.getId());
    }

    @Test
    @DisplayName("카테고리에 해당하는 게시글이 없으면 빈 페이지를 반환한다.")
    void test_find_published_posts_by_category_empty() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        postRepository.save(Post.publish("title", "summary", "content", category));

        PostSearchCondition condition = new PostSearchCondition("frontend", List.of(), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("요청한 태그를 모두 포함한 공개 게시글을 조회한다.")
    void test_find_published_posts_by_multiple_tags_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        Post javaAndSpringPost = postRepository.save(
            Post.publish(
                "Java Spring",
                "Java Spring summary",
                "Java Spring content",
                category
            )
        );
        Post javaPost = postRepository.save(
            Post.publish(
                "Java",
                "Java summary",
                "Java content",
                category
            )
        );
        Post springPost = postRepository.save(
            Post.publish(
                "Spring",
                "Spring summary",
                "Spring content",
                category
            )
        );

        postTagRepository.saveAll(List.of(
            new PostTag(javaAndSpringPost, java),
            new PostTag(javaAndSpringPost, spring),
            new PostTag(javaPost, java),
            new PostTag(springPost, spring)
        ));

        PostSearchCondition condition = new PostSearchCondition(null, List.of("java", "spring"), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(javaAndSpringPost.getId());
    }

    @Test
    @DisplayName("태그가 포함된 공개 게시글을 조회한다.")
    void test_find_published_posts_by_tag_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Post taggedPost = postRepository.save(Post.publish("tagged", "tagged summary", "tagged content", category));
        Post untaggedPost = postRepository.save(Post.publish("untagged", "untagged summary", "untagged content", category));

        postTagRepository.save(new PostTag(taggedPost, java));

        PostSearchCondition condition = new PostSearchCondition(null, List.of("java"), null);

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(taggedPost.getId())
            .doesNotContain(untaggedPost.getId());
    }

    @Test
    @DisplayName("공개 게시글 상세 정보를 조회한다.")
    void test_find_published_post_by_id_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Post post = postRepository.save(Post.publish("title", "summary", "content", category));
        PostImage thumbnail = new PostImage(
            PostImageType.THUMBNAIL,
            "thumbnail/object-key",
            "image/webp"
        );
        thumbnail.attachTo(post);

        PostImage contentImage = new PostImage(PostImageType.CONTENT, "content/object-key", "image/webp");
        contentImage.attachTo(post);

        postImageRepository.saveAll(List.of(thumbnail, contentImage));
        postTagRepository.save(new PostTag(post, java));

        Optional<PostDetailDto> result = postRepository.findPublishedPostById(post.getId());

        assertThat(result).isPresent();

        PostDetailDto detail = result.get();

        assertThat(detail.postId()).isEqualTo(post.getId());
        assertThat(detail.title()).isEqualTo("title");
        assertThat(detail.summary()).isEqualTo("summary");
        assertThat(detail.content()).isEqualTo("content");
        assertThat(detail.thumbnailImageId()).isEqualTo(thumbnail.getId());
        assertThat(detail.categoryName()).isEqualTo("Backend");
        assertThat(detail.categorySlug()).isEqualTo("backend");
        assertThat(detail.tags())
            .extracting(TagDto::slug)
            .containsExactly("java");
        assertThat(detail.contentImageIds())
            .containsExactly(contentImage.getId());
    }

    @Test
    @DisplayName("초안 게시글은 공개 상세 조회 결과에 포함하지 않는다.")
    void test_find_published_post_by_id_empty_when_draft() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post draft = postRepository.save(Post.createDraft("title", "summary", "content", category));

        Optional<PostDetailDto> result = postRepository.findPublishedPostById(draft.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 빈 결과를 반환한다.")
    void test_find_published_post_by_id_empty_when_not_found() {
        Optional<PostDetailDto> result = postRepository.findPublishedPostById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("썸네일이 없는 공개 게시글을 목록에서 조회한다.")
    void test_find_published_posts_success_without_thumbnail() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post post = postRepository.save(Post.publish("제목", "요약", "본문", category));

        Page<PostSummaryDto> result = postRepository.findPublishedPosts(
            new PostSearchCondition(
                null,
                List.of(),
                null
            ),
            PageRequest.of(0, 5)
        );

        assertThat(result.getContent())
            .extracting(PostSummaryDto::postId)
            .containsExactly(post.getId());

        assertThat(result.getContent().getFirst().thumbnailImageId())
            .isNull();
    }

    @Test
    @DisplayName("썸네일이 없는 공개 게시글을 상세 조회한다.")
    void test_find_published_post_by_id_success_without_thumbnail() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));

        Post post = postRepository.save(Post.publish("제목", "요약", "본문", category));

        PostDetailDto result = postRepository.findPublishedPostById(post.getId()).orElseThrow();

        assertThat(result.postId()).isEqualTo(post.getId());
        assertThat(result.thumbnailImageId()).isNull();
    }

    @Test
    @DisplayName("임시저장 목록을 조회하면 초안의 카테고리와 태그를 반환한다.")
    void test_find_draft_posts_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));

        Post draft = postRepository.save(
            Post.createDraft(
                "draft title",
                "draft summary",
                "draft content",
                category
            )
        );

        postTagRepository.saveAll(List.of(new PostTag(draft, spring), new PostTag(draft, java)));

        Post published = postRepository.save(
            Post.publish(
                "published title",
                "published summary",
                "published content",
                category
            )
        );

        Page<PostDraftSummaryDto> result = postRepository.findDraftPosts(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        PostDraftSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.postId()).isEqualTo(draft.getId());
        assertThat(summary.postId()).isNotEqualTo(published.getId());
        assertThat(summary.title()).isEqualTo("draft title");
        assertThat(summary.category()).isNotNull();
        assertThat(summary.category().categoryId()).isEqualTo(category.getId());
        assertThat(summary.category().name()).isEqualTo("Backend");
        assertThat(summary.category().slug()).isEqualTo("backend");
        assertThat(summary.tags()).extracting(TagDto::slug).containsExactly("java", "spring");
        assertThat(summary.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("카테고리가 없는 임시저장은 카테고리를 null로 반환한다.")
    void test_find_draft_posts_without_category_success() {
        Post draft = postRepository.save(Post.createDraft(null, null, null, null));
        Page<PostDraftSummaryDto> result = postRepository.findDraftPosts(PageRequest.of(0, 10));

        PostDraftSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.postId()).isEqualTo(draft.getId());
        assertThat(summary.title()).isEmpty();
        assertThat(summary.category()).isNull();
        assertThat(summary.tags()).isEmpty();
    }

    @Test
    @DisplayName("임시저장 목록을 한 페이지에 10개씩 조회한다.")
    void test_find_draft_posts_with_pagination_success() {
        for (int index = 1; index <= 11; index++) {
            postRepository.save(
                Post.createDraft(
                    "draft " + index,
                    null,
                    null,
                    null
                )
            );
        }

        Page<PostDraftSummaryDto> result = postRepository.findDraftPosts(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(11);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }
}
