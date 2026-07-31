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
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("썸네일 이미지가 null 이면 예외가 발생한다.")
    void test_create_fail_when_thumbnail_image_null() {
        PostCreateRequest request = createRequest(PostStatus.PUBLISHED, "Spring", List.of(), null, List.of());

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
