package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.model.Post;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PostImageServiceTest {

    @Mock
    private PostImageRepository postImageRepository;

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
        when(postImageRepository.saveAndFlush(any(PostImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostImageUploadDto result = postImageService.upload(file, PostImageType.CONTENT);

        assertThat(result.type()).isEqualTo(PostImageType.CONTENT);
        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.objectKey()).startsWith("posts/content/");
        assertThat(result.objectKey()).endsWith(".png");

        verify(garageUtil).uploadObject(eq(result.objectKey()), eq(MediaType.IMAGE_PNG_VALUE), any(byte[].class));
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

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn(MediaType.IMAGE_PNG_VALUE);
        when(tikaUtil.isImage(MediaType.IMAGE_PNG_VALUE)).thenReturn(true);
        when(postImageRepository.saveAndFlush(any(PostImage.class))).thenThrow(new DataIntegrityViolationException("fail"));

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isInstanceOf(PostException.class);

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
    @DisplayName("게시글과 연결되지 않은 이미지를 삭제하면 Garage object와 DB 데이터를 삭제한다.")
    void test_delete_success() {
        PostImage postImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", MediaType.IMAGE_PNG_VALUE);

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));

        postImageService.delete(1L);

        verify(garageUtil).deleteObject("posts/content/test.png");
        verify(postImageRepository).delete(postImage);
        verify(postImageRepository).flush();
    }

    @Test
    @DisplayName("게시글과 연결된 이미지를 삭제하면 예외가 발생한다.")
    void test_delete_fail_when_image_already_attached() {
        PostImage postImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", MediaType.IMAGE_PNG_VALUE);
        ReflectionTestUtils.setField(postImage, "post", mock(Post.class));

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));

        assertThatThrownBy(() -> postImageService.delete(1L)).isInstanceOf(PostException.class);

        verifyNoInteractions(garageUtil);
        verify(postImageRepository, never()).delete(postImage);
        verify(postImageRepository, never()).flush();
    }

    @Test
    @DisplayName("존재하지 않는 이미지를 삭제하면 예외가 발생한다.")
    void test_delete_fail_when_image_not_found() {
        when(postImageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.delete(1L)).isInstanceOf(PostException.class);

        verifyNoInteractions(garageUtil);
        verify(postImageRepository, never()).delete(any(PostImage.class));
    }

    @Test
    @DisplayName("DB 삭제에 실패하면 예외가 발생한다.")
    void test_delete_fail_when_db_delete_failed() {
        PostImage postImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", MediaType.IMAGE_PNG_VALUE);

        when(postImageRepository.findById(1L)).thenReturn(Optional.of(postImage));
        doThrow(new DataIntegrityViolationException("fail")).when(postImageRepository).flush();

        assertThatThrownBy(() -> postImageService.delete(1L)).isInstanceOf(PostException.class);

        verify(garageUtil).deleteObject("posts/content/test.png");
        verify(postImageRepository).delete(postImage);
        verify(postImageRepository).flush();
    }
}
