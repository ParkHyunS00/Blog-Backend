package com.parkhyuns00.blog.domain.post.service;

import com.parkhyuns00.blog.domain.post.event.PostImageCleanupEvent;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostImageTransactionService {

    private final PostImageRepository postImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostImageUploadDto save(PostImageType type, String objectKey, String mimeType) {
        try {
            PostImage image = postImageRepository.saveAndFlush(new PostImage(type, objectKey, mimeType));

            return PostImageUploadDto.from(image);
        } catch (DataAccessException exception) {
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

        try {
            postImageRepository.delete(postImage);
            postImageRepository.flush();
        } catch (DataAccessException exception) {
            throw new PostException(PostExceptionCode.POST_IMAGE_DELETE_FAILED, exception);
        }

        eventPublisher.publishEvent(new PostImageCleanupEvent(List.of(postImage.getObjectKey())));
    }
}
