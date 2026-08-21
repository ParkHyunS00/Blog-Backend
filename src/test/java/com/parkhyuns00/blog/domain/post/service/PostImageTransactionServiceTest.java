package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.post.event.PostImageCleanupEvent;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PostImageTransactionServiceTest {

    @Mock
    private PostImageRepository postImageRepository;

    @InjectMocks
    private PostImageTransactionService transactionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("게시글 이미지 메타데이터를 DB에 저장한다.")
    void test_save_post_image_success() {
        when(postImageRepository.saveAndFlush(any(PostImage.class)))
            .thenAnswer(invocation -> {
                PostImage image = invocation.getArgument(0);
                ReflectionTestUtils.setField(
                    image,
                    "id",
                    1L
                );
            return image;
        });

        PostImageUploadDto result =
            transactionService.save(
                PostImageType.CONTENT,
                "posts/content/test.png",
                "image/png"
            );

        assertThat(result.imageId()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo(PostImageType.CONTENT);
        assertThat(result.objectKey()).isEqualTo("posts/content/test.png");
        assertThat(result.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("게시글 이미지 메타데이터 저장에 실패하면 저장 실패 예외가 발생한다.")
    void test_save_post_image_fail() {
        when(postImageRepository.saveAndFlush(any(PostImage.class)))
            .thenThrow(new DataIntegrityViolationException("save failed"));

        assertThatThrownBy(() -> transactionService.save(
            PostImageType.CONTENT,
            "posts/content/test.png",
            "image/png"
            )
        )
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_SAVE_FAILED);
    }

    @Test
    @DisplayName("게시글에 연결되지 않은 이미지 메타데이터를 삭제하고 이미지 정리 이벤트를 발행한다.")
    void test_delete_post_image_success() {
        PostImage postImage = new PostImage(
            PostImageType.CONTENT,
            "posts/content/test.png",
            "image/png"
        );

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));

        transactionService.delete(1L);

        verify(postImageRepository).delete(postImage);
        verify(postImageRepository).flush();
        verify(eventPublisher).publishEvent(new PostImageCleanupEvent(List.of("posts/content/test.png")));
    }

    @Test
    @DisplayName("존재하지 않는 이미지를 삭제하면 예외가 발생한다.")
    void test_delete_post_image_fail_when_not_found() {
        when(postImageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(1L))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_NOT_FOUND);

        verify(postImageRepository, never()).delete(any(PostImage.class));
        verify(postImageRepository, never()).flush();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("게시글에 연결된 이미지를 삭제하면 예외가 발생한다.")
    void test_delete_post_image_fail_when_attached() {
        PostImage postImage = new PostImage(
            PostImageType.CONTENT,
            "posts/content/test.png",
            "image/png"
        );

        ReflectionTestUtils.setField(postImage, "post", mock(Post.class));

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));

        assertThatThrownBy(() -> transactionService.delete(1L))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);

        verify(postImageRepository, never()).delete(postImage);
        verify(postImageRepository, never()).flush();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("이미지 메타데이터 삭제에 실패하면 삭제 실패 예외가 발생하고 정리 이벤트를 발행하지 않는다.")
    void test_delete_post_image_fail_when_db_delete_failed() {
        PostImage postImage = new PostImage(
            PostImageType.CONTENT,
            "posts/content/test.png",
            "image/png"
        );

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));

        doThrow(new DataIntegrityViolationException("delete failed"))
            .when(postImageRepository)
            .flush();

        assertThatThrownBy(() -> transactionService.delete(1L))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_DELETE_FAILED);

        verify(postImageRepository).delete(postImage);
        verify(postImageRepository).flush();
        verifyNoInteractions(eventPublisher);
    }
}
