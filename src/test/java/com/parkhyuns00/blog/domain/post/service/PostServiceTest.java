package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.cache.PostViewDeduplicationCache;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.event.PostImageCleanupEvent;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.*;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.domain.post.repository.PostTagRepository;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.service.TagService;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.parkhyuns00.blog.util.HtmlSanitizerUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostTagRepository postTagRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TagService tagService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostViewDeduplicationCache postViewDeduplicationCache;

    @InjectMocks
    private PostService postService;

    @Spy
    private HtmlSanitizerUtil htmlSanitizerUtil;

    @Test
    @DisplayName("게시글을 생성하면 발행 상태로 저장하고 이미지와 태그를 연결한다.")
    void test_create_post_success() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            """
            <p>게시글 본문</p>
            <img src="/api/post-images/2" alt="본문 이미지">
            """,
            "Spring",
            List.of("Java"),
            1L,
            List.of(2L)
        );

        Category category = new Category("Spring", "spring");
        Tag tag = new Tag("Java", "java");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);
        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of("Java"))).thenReturn(List.of(tag));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 10L);
            return post;
        });
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));
        when(postImageRepository.findById(2L)).thenReturn(Optional.of(contentImage));

        PostCreateDto result = postService.create(request);

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(thumbnail.getPost()).isNotNull();
        assertThat(contentImage.getPost()).isNotNull();

        verify(categoryService).getOrCreateByName("Spring");
        verify(tagService).getOrCreateAllByNames(List.of("Java"));
        verify(postRepository).save(any(Post.class));
        verify(postImageRepository).findById(1L);
        verify(postImageRepository).findById(2L);
        verify(postTagRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("본문 이미지 목록이 null 이면 본문 이미지 없이 게시글을 생성한다.")
    void test_create_success_when_content_image_null() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, null);

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 10L);
            return post;
        });
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));

        PostCreateDto result = postService.create(request);

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(thumbnail.getPost()).isNotNull();

        verify(postImageRepository).findById(1L);
        verify(postImageRepository, never()).findById(2L);
        verify(postTagRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("카테고리가 null 이면 예외가 발생한다.")
    void test_create_fail_when_category_name_null() {
        PostCreateRequest request = createRequest(null, List.of(), 1L, List.of());

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CATEGORY);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("카테고리 이름이 공백이면 예외가 발생한다.")
    void test_create_fail_when_category_name_blank() {
        PostCreateRequest request = createRequest("  ", List.of(), 1L, List.of());

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CATEGORY);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("썸네일 이미지가 없어도 게시글을 생성한다.")
    void test_create_when_thumbnail_image_null() {
        PostCreateRequest request = createRequest("Spring", List.of(), null, List.of());

        Category category = new Category("Spring", "spring");

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 10L);
            return post;
        });

        PostCreateDto result = postService.create(request);

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);

        verify(postRepository).save(any(Post.class));
        verifyNoInteractions(postImageRepository);
        verify(postTagRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("본문 이미지 목록에 null 이 포함되면 예외가 발생한다.")
    void test_create_fail_when_content_image_contains_null() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, Arrays.asList(2L, null));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("본문 이미지 id 가 중복되면 예외가 발생한다.")
    void test_create_fail_when_content_image_duplicated() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, List.of(2L, 2L));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("썸네일 이미지 id 가 본문 이미지에 포함되면 예외가 발생한다.")
    void test_create_fail_when_thumbnail_id_in_content_image() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, List.of(1L));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("게시글 이미지가 존재하지 않으면 예외가 발생한다.")
    void test_create_fail_when_image_not_found() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postImageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_NOT_FOUND);

        verify(postRepository).save(any(Post.class));
        verify(postImageRepository).findById(1L);
        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("썸네일 이미지 타입이 THUMBNAIL 이 아니면 예외가 발생한다.")
    void test_create_fail_when_thumbnail_image_type_invalid() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.CONTENT, "posts/thumbnail/test.png", "image/png", 1200, 630);

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE_TYPE);

        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("본문 이미지 타입이 CONTENT 가 아니면 예외가 발생한다.")
    void test_create_fail_when_content_image_type_invalid() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            """
            <p>게시글 본문</p>
            <img src="/api/post-images/2" alt="본문 이미지">
            """,
            "Spring",
            List.of(),
            1L,
            List.of(2L)
        );

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);
        PostImage contentImage = new PostImage(PostImageType.THUMBNAIL, "posts/content/test.png", "image/png", 1200, 630);

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));
        when(postImageRepository.findById(2L)).thenReturn(Optional.of(contentImage));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE_TYPE);

        assertThat(thumbnail.getPost()).isNotNull();
        assertThat(contentImage.getPost()).isNull();

        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("이미 연결된 이미지를 사용하면 예외가 발생한다.")
    void test_create_fail_when_image_already_attached() {
        PostCreateRequest request = createRequest("Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/a.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(thumbnail, "post", mock(Post.class));

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);

        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("공개 게시글 목록 조회를 요청하면 조회 결과를 반환한다.")
    void test_get_published_posts_success() {
        PostSearchCondition condition = new PostSearchCondition("backend", List.of("java", "spring"), null);
        Pageable pageable = PageRequest.of(0, 10);
        PostSummaryDto summary = new PostSummaryDto(
            1L,
            "title",
            "summary",
            10L,
            "Backend",
            "backend",
            List.of(
                new TagDto(1L, "Java", "java"),
                new TagDto(2L, "Spring", "spring")
            ),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        Page<PostSummaryDto> expected = new PageImpl<>(List.of(summary), pageable, 1);

        when(postRepository.findPublishedPosts(condition, pageable)).thenReturn(expected);

        Page<PostSummaryDto> result = postService.getPublishedPosts(
            condition,
            pageable
        );

        assertThat(result.getContent()).containsExactly(summary);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(postRepository).findPublishedPosts(condition, pageable);
    }

    @Test
    @DisplayName("게시글을 생성하면 본문 HTML을 정제해서 저장한다.")
    void test_create_post_sanitize_content_success() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            """
            <p onclick="alert('xss')">정상 본문</p>
            <script>alert('xss')</script>
            <pre><code class="language-java">int number = 1;</code></pre>
            """,
            "Spring",
            List.of(),
            null,
            List.of()
        );

        Category category = new Category("Spring", "spring");

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postRepository.save(any(Post.class)))
            .thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 10L);
                return post;
            });

        postService.create(request);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        verify(postRepository).save(postCaptor.capture());

        Post savedPost = postCaptor.getValue();

        assertThat(savedPost.getContent())
            .contains("<p>정상 본문</p>")
            .contains("class=\"language-java\"")
            .doesNotContain("<script")
            .doesNotContain("onclick")
            .doesNotContain("alert('xss')");
    }

    @Test
    @DisplayName("HTML 정제 후 본문이 비어 있으면 게시글 생성에 실패한다.")
    void test_create_fail_when_sanitized_content_blank() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            """
            <script>alert('xss')</script>
            <iframe src="https://evil.example"></iframe>
            """,
            "Spring",
            List.of(),
            null,
            List.of()
        );

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CONTENT);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("본문 이미지 ID가 요청 이미지 ID에 없으면 게시글 생성에 실패한다.")
    void test_create_fail_when_content_image_id_missing_from_request() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            "<p>본문</p><img src=\"/api/post-images/2\" alt=\"이미지\">",
            "Spring",
            List.of(),
            null,
            List.of()
        );

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("요청 이미지 ID가 본문에서 사용되지 않으면 게시글 생성에 실패한다.")
    void test_create_fail_when_content_image_not_used() {
        PostCreateRequest request = new PostCreateRequest(
            "title",
            "summary",
            "<p>이미지가 없는 본문</p>",
            "Spring",
            List.of(),
            null,
            List.of(2L)
        );

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("작성 내용 없이 게시글 초안을 생성한다.")
    void test_create_draft_success_when_content_empty() {
        PostDraftCreateRequest request = new PostDraftCreateRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                post,
                "id",
                10L
            );
            return post;
        });

        PostCreateDto result = postService.createDraft(request);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        verify(postRepository).save(postCaptor.capture());

        Post savedPost = postCaptor.getValue();

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);

        assertThat(savedPost.getTitle()).isEmpty();
        assertThat(savedPost.getSummary()).isEmpty();
        assertThat(savedPost.getContent()).isEmpty();
        assertThat(savedPost.getCategory()).isNull();

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postImageRepository);
        verify(postTagRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("작성 내용과 이미지가 포함된 게시글 초안을 생성한다.")
    void test_create_draft_success() {
        PostDraftCreateRequest request =
            new PostDraftCreateRequest(
                "draft title",
                "draft summary",
                """
                <p onclick="alert('xss')">초안 본문</p>
                <img src="/api/post-images/2" alt="본문 이미지">
                """,
                "Spring",
                List.of("Java"),
                1L,
                List.of(2L)
            );

        Category category = new Category("Spring", "spring");
        Tag tag = new Tag("Java", "java");

        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);

        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        when(categoryService.getOrCreateByName("Spring")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of("Java"))).thenReturn(List.of(tag));
        when(postRepository.save(any(Post.class)))
            .thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(
                    post,
                    "id",
                    10L
                );
                return post;
            });
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(thumbnail));
        when(postImageRepository.findById(2L)).thenReturn(Optional.of(contentImage));

        PostCreateDto result = postService.createDraft(request);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        verify(postRepository).save(postCaptor.capture());

        Post savedDraft = postCaptor.getValue();

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);

        assertThat(savedDraft.getTitle()).isEqualTo("draft title");
        assertThat(savedDraft.getSummary()).isEqualTo("draft summary");
        assertThat(savedDraft.getContent())
            .contains("<p>초안 본문</p>")
            .contains("/api/post-images/2")
            .doesNotContain("onclick")
            .doesNotContain("alert('xss')");
        assertThat(savedDraft.getCategory()).isSameAs(category);

        assertThat(thumbnail.getPost()).isSameAs(savedDraft);
        assertThat(contentImage.getPost()).isSameAs(savedDraft);

        verify(postTagRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("초안 본문 이미지와 요청 이미지 ID가 다르면 생성에 실패한다.")
    void test_create_draft_fail_when_content_image_mismatched() {
        PostDraftCreateRequest request =
            new PostDraftCreateRequest(
                "title",
                "summary",
                "<img src=\"/api/post-images/2\" alt=\"이미지\">",
                null,
                null,
                null,
                List.of(3L)
            );

        assertThatThrownBy(() -> postService.createDraft(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("게시글 초안의 작성 내용과 카테고리를 수정한다.")
    void test_update_draft_success() {
        Long postId = 1L;
        Category oldCategory = new Category("Spring", "spring");
        Category newCategory = new Category("Java", "java");

        Post draft = Post.createDraft("old title", "old summary", "old content", oldCategory);

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            "new title",
            "new summary",
            """
            <p onclick="alert('xss')">new content</p>
            """,
            "Java",
            List.of(),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(categoryService.getOrCreateByName("Java")).thenReturn(newCategory);

        PostCreateDto result = postService.updateDraft(postId, request);

        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);
        assertThat(draft.getTitle()).isEqualTo("new title");
        assertThat(draft.getSummary()).isEqualTo("new summary");
        assertThat(draft.getContent())
            .contains("<p>new content</p>")
            .doesNotContain("onclick")
            .doesNotContain("alert('xss')");
        assertThat(draft.getCategory()).isSameAs(newCategory);

        verify(postRepository).findByIdAndStatus(postId, PostStatus.DRAFT);
    }

    @Test
    @DisplayName("수정할 게시글 초안을 찾을 수 없으면 예외가 발생한다.")
    void test_update_draft_fail_when_not_found() {
        Long postId = 999L;

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updateDraft(postId, request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("게시글 초안을 수정하면 기존 태그를 요청 태그로 교체한다.")
    void test_update_draft_replace_tags_success() {
        Long postId = 1L;
        Post draft = Post.createDraft(null, null, null, null);

        ReflectionTestUtils.setField(draft, "id", postId);

        Tag java = new Tag("Java", "java");
        Tag spring = new Tag("Spring", "spring");

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            null,
            null,
            List.of("Java", "Spring"),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(tagService.getOrCreateAllByNames(List.of("Java", "Spring"))).thenReturn(List.of(java, spring));

        postService.updateDraft(postId, request);

        verify(postTagRepository).deleteAllByPostId(postId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PostTag>> captor = ArgumentCaptor.forClass(List.class);

        verify(postTagRepository).saveAll(captor.capture());

        List<PostTag> savedPostTags = captor.getValue();

        assertThat(savedPostTags)
            .hasSize(2)
            .extracting(postTag -> postTag.getTag().getName())
            .containsExactly("Java", "Spring");

        assertThat(savedPostTags).allSatisfy(postTag -> assertThat(postTag.getPost()).isSameAs(draft));
    }

    @Test
    @DisplayName("태그 없이 게시글 초안을 수정하면 기존 태그를 모두 제거한다.")
    void test_update_draft_remove_all_tags_success() {
        Long postId = 1L;

        Post draft = Post.createDraft(null, null, null, null);

        ReflectionTestUtils.setField(draft, "id", postId);

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));

        postService.updateDraft(postId, request);

        verify(postTagRepository).deleteAllByPostId(postId);
        verifyNoInteractions(tagService);
        verify(postTagRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("이미지 없이 초안을 수정하면 기존 이미지를 모두 분리한다.")
    void test_update_draft_detach_all_images_success() {
        Long postId = 1L;
        Post draft = Post.createDraft(null, null, null, null);

        ReflectionTestUtils.setField(draft, "id", postId);

        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/old.png", "image/png", 1200, 630);

        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/old.png", "image/png", 1200, 630);

        thumbnail.attachTo(draft);
        contentImage.attachTo(draft);

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));

        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of(thumbnail, contentImage));

        postService.updateDraft(postId, request);

        assertThat(thumbnail.getPost()).isNull();
        assertThat(contentImage.getPost()).isNull();

        verify(postImageRepository).findAllByPostId(postId);
    }

    @Test
    @DisplayName("초안 수정 시 요청된 이미지는 유지하거나 새로 연결한다.")
    void test_update_draft_synchronize_images_success() {
        Long postId = 1L;
        Post draft = Post.createDraft(null, null, null, null);
        ReflectionTestUtils.setField(draft, "id", postId);

        PostImage retainedContentImage = new PostImage(PostImageType.CONTENT, "posts/content/retained.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(retainedContentImage, "id", 2L);

        retainedContentImage.attachTo(draft);

        PostImage newThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/new.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(newThumbnail, "id", 3L);

        PostImage newContentImage = new PostImage(PostImageType.CONTENT, "posts/content/new.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(newContentImage, "id", 4L);

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            """
            <img src="/api/post-images/2" alt="기존 이미지">
            <img src="/api/post-images/4" alt="새 이미지">
            """,
            null,
            null,
            3L,
            List.of(2L, 4L)
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of(retainedContentImage));

        when(postImageRepository.findById(2L)).thenReturn(Optional.of(retainedContentImage));
        when(postImageRepository.findById(3L)).thenReturn(Optional.of(newThumbnail));
        when(postImageRepository.findById(4L)).thenReturn(Optional.of(newContentImage));

        postService.updateDraft(postId, request);

        assertThat(retainedContentImage.getPost()).isSameAs(draft);
        assertThat(newThumbnail.getPost()).isSameAs(draft);
        assertThat(newContentImage.getPost()).isSameAs(draft);
    }

    @Test
    @DisplayName("다른 게시글에 연결된 이미지는 초안에 연결할 수 없다.")
    void test_update_draft_fail_when_image_attached_to_other_post() {
        Long postId = 1L;
        Post draft = Post.createDraft(null, null, null, null);
        ReflectionTestUtils.setField(draft, "id", postId);

        Post otherPost = Post.createDraft("other", "summary", "content", null);
        ReflectionTestUtils.setField(otherPost, "id", 2L);

        PostImage otherThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/other.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(otherThumbnail, "id", 3L);

        otherThumbnail.attachTo(otherPost);

        PostDraftUpdateRequest request = new PostDraftUpdateRequest(
            null,
            null,
            null,
            null,
            null,
            3L,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));

        when(postImageRepository.findById(3L)).thenReturn(Optional.of(otherThumbnail));

        assertThatThrownBy(() -> postService.updateDraft(postId, request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);

        assertThat(otherThumbnail.getPost()).isSameAs(otherPost);

        verify(postImageRepository, never()).findAllByPostId(postId);
        verify(postTagRepository, never()).deleteAllByPostId(postId);
    }

    @Test
    @DisplayName("게시글 임시저장 목록 조회 시 페이지 크기를 10으로 고정한다.")
    void test_get_draft_posts_use_fixed_page_size() {
        when(postRepository.findDraftPosts(any(Pageable.class))).thenReturn(Page.empty());

        postService.getDraftPosts(3);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(postRepository).findDraftPosts(captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("게시글 임시저장 상세 정보를 조회한다.")
    void test_get_draft_post_success() {
        LocalDateTime now = LocalDateTime.now();
        PostDraftDetailDto detail = new PostDraftDetailDto(
            1L,
            "draft title",
            "draft summary",
            "<p>draft content</p>",
            new CategoryDto(10L, "Spring", "spring"),
            List.of(new TagDto(20L, "Java", "java")),
            30L,
            List.of(31L, 32L),
            now,
            now);

        when(postRepository.findDraftPostById(1L)).thenReturn(Optional.of(detail));

        PostDraftDetailDto result = postService.getDraftPost(1L);

        assertThat(result).isEqualTo(detail);

        verify(postRepository).findDraftPostById(1L);
    }

    @Test
    @DisplayName("게시글 임시저장을 찾을 수 없으면 예외가 발생한다.")
    void test_get_draft_post_fail_when_not_found() {
        when(postRepository.findDraftPostById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getDraftPost(999L))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository).findDraftPostById(999L);
    }

    @Test
    @DisplayName("게시글 임시저장을 삭제하고 이미지 삭제 이벤트를 발행한다.")
    void test_delete_draft_success() {
        Long postId = 1L;
        Post draft = Post.createDraft("title", "summary", "content", null);

        ReflectionTestUtils.setField(draft, "id", postId);

        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);

        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of(thumbnail, contentImage));

        postService.deleteDraft(postId);

        verify(postTagRepository).deleteAllByPostId(postId);
        verify(postImageRepository).deleteAll(List.of(thumbnail, contentImage));
        verify(postImageRepository).flush();
        verify(postRepository).delete(draft);
        verify(postRepository).flush();

        ArgumentCaptor<PostImageCleanupEvent> eventCaptor = ArgumentCaptor.forClass(PostImageCleanupEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostImageCleanupEvent event = eventCaptor.getValue();

        assertThat(event.imageObjectKeys())
            .containsExactly("posts/thumbnail/test.png", "posts/content/test.png");
    }

    @Test
    @DisplayName("삭제할 게시글 임시저장을 찾을 수 없으면 예외가 발생한다.")
    void test_delete_draft_fail_when_not_found() {
        Long postId = 999L;

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deleteDraft(postId))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository, never()).delete(any(Post.class));
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(postImageRepository);
    }

    @Test
    @DisplayName("게시글 임시저장에 최종 내용을 반영하고 발행한다.")
    void test_publish_draft_success() {
        Long postId = 1L;
        Category oldCategory = new Category("Draft Category", "draft-category");
        Category newCategory = new Category("Backend", "backend");
        Post draft = Post.createDraft(
            "임시 제목",
            "임시 요약",
            "<p>임시 본문</p>",
            oldCategory
        );

        ReflectionTestUtils.setField(draft, "id", postId);

        PostCreateRequest request = new PostCreateRequest(
            "최종 제목",
            "최종 요약",
            "<p>최종 본문</p>",
            "Backend",
            List.of(),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(categoryService.getOrCreateByName("Backend")).thenReturn(newCategory);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of());

        PostCreateDto result = postService.publishDraft(postId, request);

        assertThat(result.postId()).isEqualTo(postId);
        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(draft.getTitle()).isEqualTo("최종 제목");
        assertThat(draft.getSummary()).isEqualTo("최종 요약");
        assertThat(draft.getContent()).isEqualTo("<p>최종 본문</p>");
        assertThat(draft.getCategory()).isSameAs(newCategory);
        assertThat(draft.getStatus()).isEqualTo(PostStatus.PUBLISHED);

        verify(postImageRepository).findAllByPostId(postId);
        verify(postTagRepository).deleteAllByPostId(postId);
        verify(postTagRepository).saveAll(List.of());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("발행할 게시글 임시저장을 찾을 수 없으면 예외가 발생한다.")
    void test_publish_draft_fail_when_not_found() {
        Long postId = 999L;
        PostCreateRequest request = createRequest(
            "Backend",
            List.of(),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            postService.publishDraft(postId, request)
        )
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("발행된 게시글을 삭제하고 이미지 삭제 이벤트를 발행한다.")
    void test_delete_published_post_success() {
        Long postId = 1L;
        Category category = new Category("Backend", "backend");
        Post post = Post.publish("title", "summary", "content", category);

        ReflectionTestUtils.setField(post, "id", postId);

        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png", 1200, 630);

        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED))
            .thenReturn(Optional.of(post));
        when(postImageRepository.findAllByPostId(postId))
            .thenReturn(List.of(thumbnail, contentImage));

        postService.deletePublishedPost(postId);

        verify(postTagRepository).deleteAllByPostId(postId);
        verify(postImageRepository).deleteAll(List.of(thumbnail, contentImage));
        verify(postImageRepository).flush();
        verify(postRepository).delete(post);
        verify(postRepository).flush();

        ArgumentCaptor<PostImageCleanupEvent> eventCaptor = ArgumentCaptor.forClass(PostImageCleanupEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().imageObjectKeys())
            .containsExactly("posts/thumbnail/test.png", "posts/content/test.png");
    }

    @Test
    @DisplayName("삭제할 발행된 게시글을 찾을 수 없으면 예외가 발생한다.")
    void test_delete_published_post_fail_when_not_found() {
        Long postId = 999L;

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePublishedPost(postId))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository, never()).delete(any(Post.class));
        verify(postRepository, never()).flush();
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("발행 게시글의 작성 내용을 수정한다.")
    void test_update_published_post_success() {
        Long postId = 1L;
        Category oldCategory = new Category("Spring", "spring");
        Category newCategory = new Category("Backend", "backend");

        Post post = Post.publish("기존 제목", "기존 요약", "<p>기존 본문</p>", oldCategory);

        ReflectionTestUtils.setField(post, "id", postId);

        PostCreateRequest request = new PostCreateRequest(
            "수정 제목",
            "수정 요약",
            "<p>수정 본문</p>",
            "Backend",
            List.of(),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(categoryService.getOrCreateByName("Backend")).thenReturn(newCategory);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of());

        PostCreateDto result = postService.updatePublishedPost(postId, request);

        assertThat(result.postId()).isEqualTo(postId);
        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);

        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getSummary()).isEqualTo("수정 요약");
        assertThat(post.getContent()).isEqualTo("<p>수정 본문</p>");
        assertThat(post.getCategory()).isSameAs(newCategory);
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);

        verify(postImageRepository).findAllByPostId(postId);
        verify(postTagRepository).deleteAllByPostId(postId);
        verify(postTagRepository).saveAll(List.of());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("수정할 발행 게시글을 찾을 수 없으면 예외가 발생한다.")
    void test_update_published_post_fail_when_not_found() {
        Long postId = 999L;

        PostCreateRequest request = new PostCreateRequest(
            "수정 제목",
            "수정 요약",
            "<p>수정 본문</p>",
            "Backend",
            List.of("Java"),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePublishedPost(postId, request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository).findByIdAndStatus(postId, PostStatus.PUBLISHED);
        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("발행 게시글 수정 시 요청된 이미지 관계로 동기화한다.")
    void test_update_published_post_synchronize_images_success() {
        Long postId = 1L;

        Category category = new Category("Backend", "backend");
        Post post = Post.publish("기존 제목", "기존 요약", "<p>기존 본문</p>", category);

        ReflectionTestUtils.setField(post, "id", postId);

        PostImage oldThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/old.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(oldThumbnail, "id", 1L);
        oldThumbnail.attachTo(post);

        PostImage retainedContentImage = new PostImage(PostImageType.CONTENT, "posts/content/retained.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(retainedContentImage, "id", 2L);
        retainedContentImage.attachTo(post);

        PostImage newThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/new.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(newThumbnail, "id", 3L);

        PostImage newContentImage = new PostImage(PostImageType.CONTENT, "posts/content/new.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(newContentImage, "id", 4L);

        PostCreateRequest request = new PostCreateRequest(
            "수정 제목",
            "수정 요약",
            """
            <p>수정 본문</p>
            <img src="/api/post-images/2" alt="유지 이미지">
            <img src="/api/post-images/4" alt="새 이미지">
            """,
            "Backend",
            List.of(),
            3L,
            List.of(2L, 4L)
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(categoryService.getOrCreateByName("Backend")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());

        when(postImageRepository.findById(2L)).thenReturn(Optional.of(retainedContentImage));
        when(postImageRepository.findById(3L)).thenReturn(Optional.of(newThumbnail));
        when(postImageRepository.findById(4L)).thenReturn(Optional.of(newContentImage));

        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of(oldThumbnail, retainedContentImage));

        postService.updatePublishedPost(postId, request);

        assertThat(oldThumbnail.getPost()).isNull();
        assertThat(retainedContentImage.getPost()).isSameAs(post);
        assertThat(newThumbnail.getPost()).isSameAs(post);
        assertThat(newContentImage.getPost()).isSameAs(post);

        verify(postImageRepository).findAllByPostId(postId);
    }

    @Test
    @DisplayName("발행 게시글 수정 시 기존 태그를 요청 태그로 교체한다.")
    void test_update_published_post_replace_tags_success() {
        Long postId = 1L;
        Category category = new Category("Backend", "backend");
        Post post = Post.publish("기존 제목", "기존 요약", "<p>기존 본문</p>", category);
        ReflectionTestUtils.setField(post, "id", postId);

        Tag java = new Tag("Java", "java");
        Tag spring = new Tag("Spring", "spring");

        PostCreateRequest request = new PostCreateRequest(
            "수정 제목",
            "수정 요약",
            "<p>수정 본문</p>",
            "Backend",
            List.of("Java", "Spring"),
            null,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(categoryService.getOrCreateByName("Backend")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of("Java", "Spring"))).thenReturn(List.of(java, spring));
        when(postImageRepository.findAllByPostId(postId)).thenReturn(List.of());

        postService.updatePublishedPost(postId, request);

        verify(postTagRepository).deleteAllByPostId(postId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PostTag>> captor = ArgumentCaptor.forClass(List.class);

        verify(postTagRepository).saveAll(captor.capture());

        List<PostTag> savedPostTags = captor.getValue();

        assertThat(savedPostTags)
            .hasSize(2)
            .extracting(postTag -> postTag.getTag().getName())
            .containsExactly("Java", "Spring");

        assertThat(savedPostTags).allSatisfy(postTag -> assertThat(postTag.getPost()).isSameAs(post));
    }

    @Test
    @DisplayName("다른 게시글에 연결된 이미지는 발행 게시글 수정에 사용할 수 없다.")
    void test_update_published_post_fail_when_image_attached_to_other_post() {
        Long postId = 1L;
        Category category = new Category("Backend", "backend");
        Post post = Post.publish("기존 제목", "기존 요약", "<p>기존 본문</p>", category);
        ReflectionTestUtils.setField(post, "id", postId);

        Post otherPost = Post.publish(
            "다른 제목",
            "다른 요약",
            "<p>다른 본문</p>",
            category
        );
        ReflectionTestUtils.setField(otherPost, "id", 2L);

        PostImage otherThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/other.png", "image/png", 1200, 630);
        ReflectionTestUtils.setField(otherThumbnail, "id", 3L);
        otherThumbnail.attachTo(otherPost);

        PostCreateRequest request = new PostCreateRequest(
            "수정 제목",
            "수정 요약",
            "<p>수정 본문</p>",
            "Backend",
            List.of(),
            3L,
            List.of()
        );

        when(postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(categoryService.getOrCreateByName("Backend")).thenReturn(category);
        when(tagService.getOrCreateAllByNames(List.of())).thenReturn(List.of());
        when(postImageRepository.findById(3L)).thenReturn(Optional.of(otherThumbnail));

        assertThatThrownBy(() ->
            postService.updatePublishedPost(postId, request)
        )
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);

        assertThat(otherThumbnail.getPost()).isSameAs(otherPost);

        verify(postImageRepository, never()).findAllByPostId(postId);
        verify(postTagRepository, never()).deleteAllByPostId(postId);
        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("방문자가 공개 게시글을 처음 조회하면 조회수를 증가시킨다.")
    void test_get_published_post_success_when_first_view() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();
        PostDetailDto detail = createPostDetail(10L);

        when(postRepository.findPublishedPostById(postId)).thenReturn(Optional.of(detail));
        when(postViewDeduplicationCache.reserve(postId, visitorId)).thenReturn(true);
        when(postRepository.incrementViewCount(postId)).thenReturn(1);

        PostDetailDto result = postService.getPublishedPost(postId, visitorId);

        assertThat(result.viewCount()).isEqualTo(11L);

        verify(postViewDeduplicationCache).reserve(postId, visitorId);
        verify(postRepository).incrementViewCount(postId);
        verify(postRepository).findPublishedPostById(postId);
    }

    @Test
    @DisplayName("동일한 방문자가 공개 게시글을 다시 조회하면 조회수를 증가시키지 않는다.")
    void test_get_published_post_success_when_duplicate_view() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();
        PostDetailDto detail = createPostDetail(10L);

        when(postRepository.findPublishedPostById(postId)).thenReturn(Optional.of(detail));
        when(postViewDeduplicationCache.reserve(postId, visitorId)).thenReturn(false);

        PostDetailDto result = postService.getPublishedPost(postId, visitorId);

        assertThat(result.viewCount()).isEqualTo(10L);

        verify(postViewDeduplicationCache).reserve(postId, visitorId);
        verify(postRepository, never()).incrementViewCount(postId);
    }

    @Test
    @DisplayName("공개 게시글을 찾을 수 없으면 예외가 발생한다.")
    void test_get_published_post_fail_when_post_not_found() {
        Long postId = 999L;
        UUID visitorId = UUID.randomUUID();

        when(postRepository.findPublishedPostById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublishedPost(postId, visitorId))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository).findPublishedPostById(postId);
        verifyNoInteractions(postViewDeduplicationCache);
        verify(postRepository, never()).incrementViewCount(postId);
    }

    @Test
    @DisplayName("조회수 증가에 실패하면 캐시 예약을 해제한다.")
    void test_get_published_post_release_cache_when_increment_failed() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();
        PostDetailDto detail = createPostDetail(10L);
        DataAccessResourceFailureException exception = new DataAccessResourceFailureException("조회수 증가 실패");

        when(postRepository.findPublishedPostById(postId)).thenReturn(Optional.of(detail));
        when(postViewDeduplicationCache.reserve(postId, visitorId)).thenReturn(true);
        when(postRepository.incrementViewCount(postId)).thenThrow(exception);

        assertThatThrownBy(() -> postService.getPublishedPost(postId, visitorId)).isSameAs(exception);

        verify(postViewDeduplicationCache).release(postId, visitorId);
    }

    @Test
    @DisplayName("조회수 증가 대상이 사라지면 캐시 예약을 해제하고 예외가 발생한다.")
    void test_get_published_post_fail_when_increment_target_not_found() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();
        PostDetailDto detail = createPostDetail(10L);

        when(postRepository.findPublishedPostById(postId)).thenReturn(Optional.of(detail));
        when(postViewDeduplicationCache.reserve(postId, visitorId)).thenReturn(true);
        when(postRepository.incrementViewCount(postId)).thenReturn(0);

        assertThatThrownBy(() -> postService.getPublishedPost(postId, visitorId))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postViewDeduplicationCache).release(postId, visitorId);
    }

    private PostDetailDto createPostDetail(long viewCount) {
        LocalDateTime now = LocalDateTime.now();

        return new PostDetailDto(
            1L,
            "title",
            "summary",
            "content",
            viewCount,
            null,
            "Backend",
            "backend",
            List.of(),
            List.of(),
            now,
            now
        );
    }

    private PostCreateRequest createRequest(
        String categoryName,
        List<String> tagNames,
        Long thumbnailImageId,
        List<Long> contentImageIds
    ) {
        return new PostCreateRequest(
            "title",
            "summary",
            "content",
            categoryName,
            tagNames,
            thumbnailImageId,
            contentImageIds
        );
    }
}
