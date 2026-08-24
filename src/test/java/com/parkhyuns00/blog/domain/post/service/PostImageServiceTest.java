package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageDownloadDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import com.parkhyuns00.blog.util.GarageUtil;
import com.parkhyuns00.blog.util.TikaUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PostImageServiceTest {

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostImageTransactionService transactionService;

    @Mock
    private GarageUtil garageUtil;

    @Mock
    private TikaUtil tikaUtil;

    @InjectMocks
    private PostImageService postImageService;

    @Test
    @DisplayName("이미지 파일을 업로드하면 Garage에 저장하고 PostImage 정보를 저장한다.")
    void test_image_upload_success() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "image-png".getBytes()
        );

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn(MediaType.IMAGE_PNG_VALUE);
        when(tikaUtil.isImage(MediaType.IMAGE_PNG_VALUE)).thenReturn(true);
        when(
            transactionService.save(
                eq(PostImageType.CONTENT),
                startsWith("posts/content/"),
                eq(MediaType.IMAGE_PNG_VALUE)
            )
        ).thenAnswer(invocation ->
            new PostImageUploadDto(
                1L,
                PostImageType.CONTENT,
                invocation.getArgument(1),
                MediaType.IMAGE_PNG_VALUE
            )
        );

        PostImageUploadDto result = postImageService.upload(file, PostImageType.CONTENT);

        assertThat(result.type()).isEqualTo(PostImageType.CONTENT);
        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.objectKey()).startsWith("posts/content/");
        assertThat(result.objectKey()).endsWith(".png");

        verify(garageUtil).uploadObject(eq(result.objectKey()), eq(MediaType.IMAGE_PNG_VALUE), any(byte[].class));
        verify(transactionService).save(eq(PostImageType.CONTENT), eq(result.objectKey()), eq(MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    @DisplayName("빈 파일을 업로드하면 예외가 발생한다.")
    void test_upload_fail_when_empty_file() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[0]
        );

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isInstanceOf(PostException.class);

        verifyNoInteractions(tikaUtil);
        verifyNoInteractions(garageUtil);
        verifyNoInteractions(postImageRepository);
    }

    @Test
    @DisplayName("이미지 파일이 아니면 예외가 발생한다.")
    void test_upload_fail_when_invalid_mime_type() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "test.txt".getBytes()
        );

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn(MediaType.TEXT_PLAIN_VALUE);
        when(tikaUtil.isImage(MediaType.TEXT_PLAIN_VALUE)).thenReturn(false);

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isInstanceOf(PostException.class);

        verifyNoInteractions(garageUtil);
        verifyNoInteractions(postImageRepository);
    }

    @Test
    @DisplayName("지원하지 않는 이미지 형식이면 예외가 발생한다.")
    void test_upload_fail_with_unsupported_image_type() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.bmp",
            "image/bmp",
            "image-content".getBytes()
        );

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn("image/bmp");
        when(tikaUtil.isImage("image/bmp")).thenReturn(true);

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isInstanceOf(PostException.class);

        verifyNoInteractions(garageUtil);
        verifyNoInteractions(postImageRepository);
    }

    @Test
    @DisplayName("DB 저장에 실패하면 업로드된 object를 삭제한다.")
    void test_upload_delete_object_when_db_save_failed() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "image-png".getBytes()
        );

        PostException saveException = new PostException(PostExceptionCode.POST_IMAGE_SAVE_FAILED);

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn(MediaType.IMAGE_PNG_VALUE);
        when(tikaUtil.isImage(MediaType.IMAGE_PNG_VALUE)).thenReturn(true);
        when(transactionService.save(
            eq(PostImageType.CONTENT),
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE)
        )).thenThrow(saveException);

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isSameAs(saveException);

        verify(garageUtil).uploadObject(
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE),
            any(byte[].class)
        );
        verify(garageUtil).deleteObject(startsWith("posts/content/"));
    }

    @Test
    @DisplayName("이미지를 다운로드하면 Garage에서 object stream을 조회한다.")
    void test_download_success() {
        PostImage postImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", MediaType.IMAGE_PNG_VALUE);

        ResponseInputStream<GetObjectResponse> inputStream = mock(ResponseInputStream.class);
        GetObjectResponse response = GetObjectResponse.builder().contentLength(10L).build();

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));
        when(garageUtil.downloadObject("posts/content/test.png")).thenReturn(inputStream);
        when(inputStream.response()).thenReturn(response);

        PostImageDownloadDto result = postImageService.download(1L);

        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.contentLength()).isEqualTo(10L);
        assertThat(result.inputStream()).isSameAs(inputStream);

        verify(garageUtil).downloadObject("posts/content/test.png");
    }

    @Test
    @DisplayName("존재하지 않는 이미지를 다운로드하면 예외가 발생한다.")
    void test_download_fail_when_image_not_found() {
        when(postImageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.download(1L)).isInstanceOf(PostException.class);

        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("DB 저장과 보상 삭제가 모두 실패하면 DB 저장 예외를 유지하고 삭제 예외를 suppressed 예외로 추가한다.")
    void test_upload_preserve_original_exception_when_compensation_failed() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "image-png".getBytes()
        );

        PostException saveException = new PostException(PostExceptionCode.POST_IMAGE_SAVE_FAILED);
        RuntimeException compensationException = new RuntimeException("Garage object 삭제 실패");

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn(MediaType.IMAGE_PNG_VALUE);
        when(tikaUtil.isImage(MediaType.IMAGE_PNG_VALUE)).thenReturn(true);
        when(transactionService.save(
            eq(PostImageType.CONTENT),
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE)
        )).thenThrow(saveException);

        doThrow(compensationException).when(garageUtil).deleteObject(startsWith("posts/content/"));

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT))
            .isSameAs(saveException)
            .satisfies(
                exception ->
                    assertThat(exception.getSuppressed()).containsExactly(compensationException)
            );

        verify(garageUtil).uploadObject(
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE),
            any(byte[].class)
        );
        verify(garageUtil).deleteObject(startsWith("posts/content/"));
    }

    @Test
    @DisplayName("게시글 이미지 삭제를 트랜잭션 서비스에 위임한다.")
    void test_delete_post_image_success() {
        postImageService.delete(1L);

        verify(transactionService).delete(1L);
        verifyNoInteractions(garageUtil);
    }
}
