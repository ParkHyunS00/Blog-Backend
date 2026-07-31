package com.parkhyuns00.blog.domain.post.service;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageDownloadDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import com.parkhyuns00.blog.util.GarageUtil;
import com.parkhyuns00.blog.util.TikaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private final PostImageRepository postImageRepository;
    private final GarageUtil garageUtil;
    private final TikaUtil tikaUtil;

    @Transactional
    public PostImageUploadDto upload(MultipartFile file, PostImageType type) {
        validateFile(file);

        byte[] content = getBytes(file);
        String mimeType = tikaUtil.detectMimeType(content);

        validateImageMimeType(mimeType);

        String objectKey = generateObjectKey(type, mimeType);
        garageUtil.uploadObject(objectKey, mimeType, content);

        try {
            PostImage postImage = postImageRepository.saveAndFlush(new PostImage(type, objectKey, mimeType));

            return PostImageUploadDto.from(postImage);
        } catch (DataAccessException exception) {
            garageUtil.deleteObject(objectKey);
            throw new PostException(PostExceptionCode.POST_IMAGE_SAVE_FAILED, exception);
        }
    }

    @Transactional
    public void delete(Long imageId) {
        PostImage postImage = postImageRepository.findById(imageId)
            .orElseThrow(() -> new PostException(PostExceptionCode.POST_IMAGE_NOT_FOUND));

        if (postImage.isAttached()) {
            throw new PostException(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);
        }

        garageUtil.deleteObject(postImage.getObjectKey());

        try {
            postImageRepository.delete(postImage);
            postImageRepository.flush();
        } catch (DataAccessException exception) {
            throw new PostException(PostExceptionCode.POST_IMAGE_DELETE_FAILED, exception);
        }
    }

    public PostImageDownloadDto download(Long imageId) {
        PostImage postImage = postImageRepository.findById(imageId)
            .orElseThrow(() -> new PostException(PostExceptionCode.POST_IMAGE_NOT_FOUND));

        ResponseInputStream<GetObjectResponse> inputStream = garageUtil.downloadObject(postImage.getObjectKey());

        return PostImageDownloadDto.from(postImage, inputStream);
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new PostException(PostExceptionCode.POST_IMAGE_READ_FAILED, exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PostException(PostExceptionCode.POST_IMAGE_EMPTY);
        }
    }

    private void validateImageMimeType(String mimeType) {
        if (!tikaUtil.isImage(mimeType)) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE_MIME_TYPE);
        }
    }

    private String resolveExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw new PostException(PostExceptionCode.UNSUPPORTED_POST_IMAGE_TYPE);
        };
    }

    private String generateObjectKey(PostImageType type, String mimeType) {
        String extension = resolveExtension(mimeType);
        return "posts/%s/%s.%s".formatted(type.name().toLowerCase(), UUID.randomUUID(), extension);
    }
}
