package com.parkhyuns00.blog.domain.post.service;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostImageTransactionService {

    private final PostImageRepository postImageRepository;

    @Transactional
    public PostImageUploadDto save(PostImageType type, String objectKey, String mimeType) {
        try {
            PostImage image = postImageRepository.saveAndFlush(new PostImage(type, objectKey, mimeType));

            return PostImageUploadDto.from(image);
        } catch (DataAccessException exception) {
            throw new PostException(PostExceptionCode.POST_IMAGE_SAVE_FAILED, exception);
        }
    }
}
