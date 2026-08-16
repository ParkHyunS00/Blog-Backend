package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.domain.post.repository.PostTagRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private PostService postService;

    @Spy
    private HtmlSanitizerUtil htmlSanitizerUtil;

    @Test
    @DisplayName("게시글을 발행 상태로 생성하면 게시글, 이미지, 태그가 연결된다.")
    void test_create_published_post_success() {
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of("Java"), 1L, List.of(2L));

        Category category = new Category("Spring", "spring");
        Tag tag = new Tag("Java", "java");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png");
        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png");

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
    @DisplayName("게시글을 초안 상태로 생성하면 DRAFT 상태로 저장된다.")
    void test_create_draft_post_success() {
        PostCreateRequest request = createRequest(PostStatus.DRAFT, "Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png");

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
        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);
        assertThat(thumbnail.getPost()).isNotNull();

        verify(postRepository).save(any(Post.class));
        verify(postImageRepository).findById(1L);
        verify(postTagRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("본문 이미지 목록이 null 이면 본문 이미지 없이 게시글을 생성한다.")
    void test_create_success_when_content_image_null() {
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, null);

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png");

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
    @DisplayName("게시글 상태가 null 이면 예외가 발생한다.")
    void test_create_fail_when_status_null() {
        PostCreateRequest request = createRequest(null, "Spring", List.of(), 1L, List.of());

        assertThatThrownBy(() -> postService.create(request))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_STATUS);

        verifyNoInteractions(categoryService);
        verifyNoInteractions(tagService);
        verifyNoInteractions(postRepository);
        verifyNoInteractions(postImageRepository);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("카테고리가 null 이면 예외가 발생한다.")
    void test_create_fail_when_category_name_null() {
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, null, List.of(), 1L, List.of());

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "  ", List.of(), 1L, List.of());

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), null, List.of());

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, Arrays.asList(2L, null));

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of(2L, 2L));

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of(1L));

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of());

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.CONTENT, "posts/thumbnail/test.png", "image/png");

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of(2L));

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/test.png", "image/png");
        PostImage contentImage = new PostImage(PostImageType.THUMBNAIL, "posts/content/test.png", "image/png");

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
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), 1L, List.of());

        Category category = new Category("Spring", "spring");
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/a.png", "image/png");
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
    @DisplayName("공개 게시글 상세 조회를 요청하면 게시글 정보를 반환한다.")
    void test_get_published_post_success() {
        LocalDateTime now = LocalDateTime.now();
        PostDetailDto detail = new PostDetailDto(
            1L,
            "title",
            "summary",
            "content",
            10L,
            "Backend",
            "backend",
            List.of(
                new TagDto(1L, "Java", "java")
            ),
            List.of(11L, 12L),
            now,
            now
        );

        when(postRepository.findPublishedPostById(1L)).thenReturn(Optional.of(detail));

        PostDetailDto result = postService.getPublishedPost(1L);

        assertThat(result).isEqualTo(detail);

        verify(postRepository).findPublishedPostById(1L);
    }

    @Test
    @DisplayName("공개 게시글을 찾을 수 없으면 예외가 발생한다.")
    void test_get_published_post_fail_when_not_found() {
        when(postRepository.findPublishedPostById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublishedPost(999L))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_NOT_FOUND);

        verify(postRepository).findPublishedPostById(999L);
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
            PostStatus.PUBLISHED,
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
            PostStatus.PUBLISHED,
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
            PostStatus.PUBLISHED,
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
            PostStatus.PUBLISHED,
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

    private PostCreateRequest createRequest(
        PostStatus status,
        String categoryName,
        List<String> tagNames,
        Long thumbnailImageId,
        List<Long> contentImageIds
    ) {
        return new PostCreateRequest(
            "title",
            "summary",
            "content",
            status,
            categoryName,
            tagNames,
            thumbnailImageId,
            contentImageIds
        );
    }
}
