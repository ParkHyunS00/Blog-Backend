package com.parkhyuns00.blog.domain.post.service;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostTag;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.domain.post.repository.PostTagRepository;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PostImageRepository postImageRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    @Transactional
    public PostCreateDto create(PostCreateRequest request) {
        List<Long> contentImageIds = normalizeContentImageIds(request.contentImageIds());
        validateCreateRequest(request, contentImageIds);

        Category category = categoryService.getOrCreateByName(request.categoryName());
        List<Tag> tags = tagService.getOrCreateAllByNames(request.tagNames());

        Post post = createPost(request, category);
        Post savedPost = postRepository.save(post);

        attachThumbnailImage(savedPost, request.thumbnailImageId());
        attachContentImages(savedPost, contentImageIds);
        savePostTags(savedPost, tags);

        return PostCreateDto.from(savedPost);
    }

    private List<Long> normalizeContentImageIds(List<Long> contentImageIds) {
        return contentImageIds == null ? List.of() : contentImageIds;
    }

    private Post createPost(PostCreateRequest request, Category category) {
        return switch (request.status()) {
            case DRAFT -> Post.createDraft(request.title(), request.summary(), request.content(), category);
            case PUBLISHED -> Post.publish(request.title(), request.summary(), request.content(), category);
        };
    }

    private void savePostTags(Post post, List<Tag> tags) {
        List<PostTag> postTags = tags.stream()
            .map(tag -> new PostTag(post, tag))
            .toList();

        postTagRepository.saveAll(postTags);
    }

    private void attachThumbnailImage(Post post, Long thumbnailImageId) {
        PostImage thumbnail = getPostImage(thumbnailImageId);
        validateImageType(thumbnail, PostImageType.THUMBNAIL);
        thumbnail.attachTo(post);
    }

    private void attachContentImages(Post post, List<Long> contentImageIds) {
        for (Long imageId : contentImageIds) {
            PostImage image = getPostImage(imageId);
            validateImageType(image, PostImageType.CONTENT);
            image.attachTo(post);
        }
    }

    private PostImage getPostImage(Long imageId) {
        return postImageRepository.findById(imageId)
            .orElseThrow(() -> new PostException(PostExceptionCode.POST_IMAGE_NOT_FOUND));
    }

    private void validateCreateRequest(PostCreateRequest request, List<Long> contentImageIds) {
        if (request.status() == null) {
            throw new PostException(PostExceptionCode.INVALID_POST_STATUS);
        }

        if (request.categoryName() == null || request.categoryName().isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_CATEGORY);
        }

        validateImageIds(request.thumbnailImageId(), contentImageIds);
    }

    private void validateImageType(PostImage image, PostImageType expectedType) {
        if (image.getType() != expectedType) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE_TYPE);
        }
    }

    private void validateImageIds(Long thumbnailImageId, List<Long> contentImageIds) {
        if (thumbnailImageId == null) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }

        if (contentImageIds.stream().anyMatch(Objects::isNull)) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }

        if (contentImageIds.size() != contentImageIds.stream().distinct().count()) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }

        if (contentImageIds.contains(thumbnailImageId)) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }
    }
}
