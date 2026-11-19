package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import com.parkhyuns00.blog.config.security.token.AdminOtpAuthenticationToken;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageDownloadDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import com.parkhyuns00.blog.util.GarageUtil;
import com.parkhyuns00.blog.util.ImageMetadataUtil;
import com.parkhyuns00.blog.util.TikaUtil;
import com.parkhyuns00.blog.util.dto.ImageMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Mock
    private ImageMetadataUtil imageMetadataUtil;

    @InjectMocks
    private PostImageService postImageService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
        when(imageMetadataUtil.extract(any(byte[].class))).thenReturn(new ImageMetadata(1200, 630));
        when(
            transactionService.save(
                eq(PostImageType.CONTENT),
                startsWith("posts/content/"),
                eq(MediaType.IMAGE_PNG_VALUE),
                eq(1200),
                eq(630)
            )
        ).thenAnswer(invocation ->
            new PostImageUploadDto(
                1L,
                PostImageType.CONTENT,
                invocation.getArgument(1),
                MediaType.IMAGE_PNG_VALUE,
                1200,
                630
            )
        );

        PostImageUploadDto result = postImageService.upload(file, PostImageType.CONTENT);

        assertThat(result.type()).isEqualTo(PostImageType.CONTENT);
        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.objectKey()).startsWith("posts/content/");
        assertThat(result.objectKey()).endsWith(".png");
        assertThat(result.width()).isEqualTo(1200);
        assertThat(result.height()).isEqualTo(630);

        verify(garageUtil).uploadObject(eq(result.objectKey()), eq(MediaType.IMAGE_PNG_VALUE), any(byte[].class));
        verify(transactionService).save(
            eq(PostImageType.CONTENT),
            eq(result.objectKey()),
            eq(MediaType.IMAGE_PNG_VALUE),
            eq(1200),
            eq(630)
        );
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
        when(imageMetadataUtil.extract(any(byte[].class))).thenReturn(new ImageMetadata(1200, 630));
        when(transactionService.save(
            eq(PostImageType.CONTENT),
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE),
            eq(1200),
            eq(630)
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
    @DisplayName("익명 사용자는 공개 게시글 이미지만 조회한 뒤 Garage에서 object stream을 받는다.")
    void testDownloadSuccess() {
        PostImage postImage = new PostImage(PostImageType.CONTENT, "posts/content/test.png", MediaType.IMAGE_PNG_VALUE, 1200, 630);

        ResponseInputStream<GetObjectResponse> inputStream = mock(ResponseInputStream.class);
        GetObjectResponse response = GetObjectResponse.builder().contentLength(10L).build();

        when(postImageRepository.findByIdAndPostStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.of(postImage));
        when(garageUtil.downloadObject("posts/content/test.png")).thenReturn(inputStream);
        when(inputStream.response()).thenReturn(response);

        PostImageDownloadDto result = postImageService.download(1L);

        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.contentLength()).isEqualTo(10L);
        assertThat(result.inputStream()).isSameAs(inputStream);

        verify(garageUtil).downloadObject("posts/content/test.png");
        verify(postImageRepository, never()).findById(any());
    }

    @Test
    @DisplayName("공개 조회 결과가 없는 이미지는 파일 저장소 접근 없이 이미지 없음 예외를 반환한다.")
    void testDownloadFailWhenImageNotFound() {
        when(postImageRepository.findByIdAndPostStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.download(1L))
            .isInstanceOf(PostException.class)
            .hasMessage(PostExceptionCode.POST_IMAGE_NOT_FOUND.getMessage());

        verifyNoInteractions(garageUtil);
        verify(postImageRepository, never()).findById(any());
    }

    @Test
    @DisplayName("OTP 인증이 완료된 관리자는 미첨부 이미지도 다운로드할 수 있다.")
    void testAdminDownloadUnattachedImage() {
        AdminPrincipal principal = new AdminPrincipal(AdminRole.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            new AdminOtpAuthenticationToken(principal, principal.getAuthorities()));
        PostImage image = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/private.png", MediaType.IMAGE_PNG_VALUE, 800, 400);
        ResponseInputStream<GetObjectResponse> inputStream = mock(ResponseInputStream.class);
        when(postImageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(garageUtil.downloadObject(image.getObjectKey())).thenReturn(inputStream);
        when(inputStream.response()).thenReturn(GetObjectResponse.builder().contentLength(10L).build());

        PostImageDownloadDto result = postImageService.download(1L);

        assertThat(result.inputStream()).isSameAs(inputStream);
        verify(postImageRepository, never()).findByIdAndPostStatus(any(), any());
    }

    @Test
    @DisplayName("관리자 키만 인증한 PRE_ADMIN은 비공개 이미지를 조회할 수 없다.")
    void testPreAdminCannotBypassPublicImageLookup() {
        AdminPrincipal principal = new AdminPrincipal(AdminRole.PRE_ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            new AdminKeyAuthenticationToken(principal, principal.getAuthorities()));
        when(postImageRepository.findByIdAndPostStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.download(1L))
            .isInstanceOf(PostException.class)
            .hasMessage(PostExceptionCode.POST_IMAGE_NOT_FOUND.getMessage());

        verify(postImageRepository, never()).findById(any());
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("인증되지 않은 관리자 principal은 비공개 이미지 조회를 허용하지 않는다.")
    void testUnauthenticatedAdminCannotBypassPublicImageLookup() {
        AdminPrincipal principal = new AdminPrincipal(AdminRole.ADMIN);
        AdminOtpAuthenticationToken authentication = new AdminOtpAuthenticationToken(principal, principal.getAuthorities());
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(postImageRepository.findByIdAndPostStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.download(1L)).isInstanceOf(PostException.class);

        verify(postImageRepository, never()).findById(any());
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("이미지 공개 여부 조회가 실패하면 파일 저장소에 접근하지 않고 오류를 전파한다.")
    void testImageAccessQueryFailurePropagates() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
        when(postImageRepository.findByIdAndPostStatus(1L, PostStatus.PUBLISHED)).thenThrow(failure);

        assertThatThrownBy(() -> postImageService.download(1L)).isSameAs(failure);

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
        when(imageMetadataUtil.extract(any(byte[].class))).thenReturn(new ImageMetadata(1200, 630));
        when(transactionService.save(
            eq(PostImageType.CONTENT),
            startsWith("posts/content/"),
            eq(MediaType.IMAGE_PNG_VALUE),
            eq(1200),
            eq(630)
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

    @Test
    @DisplayName("이미지 메타데이터를 읽을 수 없으면 Garage와 DB에 저장하지 않는다.")
    void test_upload_fail_when_image_metadata_read_failed() {
        byte[] content = "invalid-image-content".getBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "broken.webp",
            "image/webp",
            content
        );

        PostException metadataException = new PostException(PostExceptionCode.POST_IMAGE_METADATA_READ_FAILED);

        when(tikaUtil.detectMimeType(any(byte[].class))).thenReturn("image/webp");
        when(tikaUtil.isImage("image/webp")).thenReturn(true);
        when(imageMetadataUtil.extract(any(byte[].class))).thenThrow(metadataException);

        assertThatThrownBy(() -> postImageService.upload(file, PostImageType.CONTENT)).isSameAs(metadataException);

        verify(tikaUtil).detectMimeType(content);
        verify(tikaUtil).isImage("image/webp");
        verify(imageMetadataUtil).extract(content);

        verifyNoInteractions(garageUtil);
        verifyNoInteractions(transactionService);
    }
}
