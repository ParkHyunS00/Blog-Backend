package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PostImageTransactionServiceTest {

    @Mock
    private PostImageRepository postImageRepository;

    @InjectMocks
    private PostImageTransactionService
        transactionService;

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
}
