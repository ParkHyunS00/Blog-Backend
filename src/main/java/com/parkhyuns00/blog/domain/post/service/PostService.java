package com.parkhyuns00.blog.domain.post.service;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.*;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.domain.post.repository.PostTagRepository;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.service.TagService;
import com.parkhyuns00.blog.util.HtmlSanitizerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final Pattern CONTENT_IMAGE_PATTERN = Pattern.compile("src=\"/api/post-images/([1-9][0-9]*)\"");
    private static final int DRAFT_PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PostImageRepository postImageRepository;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final HtmlSanitizerUtil htmlSanitizerUtil;

    @Transactional
    public PostCreateDto create(PostCreateRequest request) {
        List<Long> contentImageIds = normalizeContentImageIds(request.contentImageIds());
        validateCreateRequest(request, contentImageIds);

        String sanitizedContent = htmlSanitizerUtil.sanitize(request.content());
        validateSanitizedContent(sanitizedContent);
        validateContentImageIds(sanitizedContent, contentImageIds);

        Category category = categoryService.getOrCreateByName(request.categoryName());
        List<Tag> tags = tagService.getOrCreateAllByNames(request.tagNames());

        Post post = createPost(request, category, sanitizedContent);
        Post savedPost = postRepository.save(post);

        attachThumbnailImageIfPresent(savedPost, request.thumbnailImageId());
        attachContentImages(savedPost, contentImageIds);
        savePostTags(savedPost, tags);

        return PostCreateDto.from(savedPost);
    }

    @Transactional
    public PostCreateDto createDraft(PostDraftCreateRequest request) {
        List<Long> contentImageIds = normalizeContentImageIds(request.contentImageIds());
        validateImageIds(request.thumbnailImageId(), contentImageIds);

        String sanitizedContent = sanitizeDraftContent(request.content());

        validateContentImageIds(sanitizedContent, contentImageIds);

        Category category = getDraftCategory(request.categoryName());
        List<Tag> tags = getDraftTags(request.tagNames());
        Post draft = Post.createDraft(request.title(), request.summary(), sanitizedContent, category);

        Post savedDraft = postRepository.save(draft);

        attachThumbnailImageIfPresent(savedDraft, request.thumbnailImageId());
        attachContentImages(savedDraft, contentImageIds);

        savePostTags(savedDraft, tags);

        return PostCreateDto.from(savedDraft);
    }

    @Transactional
    public PostCreateDto updateDraft(Long postId, PostDraftUpdateRequest request) {
        Post draft = postRepository.findByIdAndStatus(postId, PostStatus.DRAFT)
            .orElseThrow(() -> new PostException(PostExceptionCode.POST_NOT_FOUND));

        List<Long> contentImageIds = normalizeContentImageIds(request.contentImageIds());
        validateImageIds(request.thumbnailImageId(), contentImageIds);

        String sanitizedContent = sanitizeDraftContent(request.content());
        validateContentImageIds(sanitizedContent, contentImageIds);

        Category category = getDraftCategory(request.categoryName());
        List<Tag> tags = getDraftTags(request.tagNames());

        draft.updateDraft(request.title(), request.summary(), sanitizedContent, category);

        synchronizeDraftImages(draft, request.thumbnailImageId(), contentImageIds);

        replacePostTags(draft, tags);

        return PostCreateDto.from(draft);
    }

    public Page<PostDraftSummaryDto> getDraftPosts(int page) {
        Pageable pageable = PageRequest.of(page, DRAFT_PAGE_SIZE);
        return postRepository.findDraftPosts(pageable);
    }

    public Page<PostSummaryDto> getPublishedPosts(PostSearchCondition condition, Pageable pageable) {
        return postRepository.findPublishedPosts(condition, pageable);
    }

    public PostDetailDto getPublishedPost(Long postId) {
        return postRepository.findPublishedPostById(postId)
            .orElseThrow(() -> new PostException(PostExceptionCode.POST_NOT_FOUND));
    }

    private void synchronizeDraftImages(Post draft, Long thumbnailImageId, List<Long> contentImageIds) {
        PostImage thumbnail = resolveThumbnailImage(thumbnailImageId, draft);
        List<PostImage> contentImages = resolveContentImages(contentImageIds, draft);
        List<PostImage> currentImages = postImageRepository.findAllByPostId(draft.getId());

        Set<Long> requestedImageIds = new HashSet<>(contentImageIds);

        if (thumbnailImageId != null) {
            requestedImageIds.add(thumbnailImageId);
        }

        currentImages.stream()
            .filter(image -> !requestedImageIds.contains(image.getId()))
            .forEach(PostImage::detach);

        attachIfNecessary(thumbnail, draft);

        contentImages.forEach(image -> attachIfNecessary(image, draft));
    }

    private PostImage resolveThumbnailImage(Long imageId, Post draft) {
        if (imageId == null) return null;

        PostImage image = getPostImage(imageId);

        validateImageType(image, PostImageType.THUMBNAIL);
        validateImageAttachable(image, draft);

        return image;
    }

    private List<PostImage> resolveContentImages(List<Long> imageIds, Post draft) {
        return imageIds.stream()
            .map(this::getPostImage)
            .peek(image -> {
                validateImageType(image, PostImageType.CONTENT);
                validateImageAttachable(image, draft);
            })
            .toList();
    }

    private void validateImageAttachable(PostImage image, Post draft) {
        if (image.isAttached() && !image.isAttachedTo(draft)) {
            throw new PostException(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);
        }
    }

    private void attachIfNecessary(PostImage image, Post draft) {
        if (image == null || image.isAttachedTo(draft)) return;

        image.attachTo(draft);
    }

    private void replacePostTags(Post draft, List<Tag> tags) {
        postTagRepository.deleteAllByPostId(draft.getId());

        savePostTags(draft, tags);
    }

    private Category getDraftCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;

        return categoryService.getOrCreateByName(categoryName);
    }

    private List<Tag> getDraftTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        return tagService.getOrCreateAllByNames(tagNames);
    }

    private String sanitizeDraftContent(String content) {
        if (content == null || content.isBlank()) return "";

        return htmlSanitizerUtil.sanitize(content);
    }

    private List<Long> normalizeContentImageIds(List<Long> contentImageIds) {
        return contentImageIds == null ? List.of() : contentImageIds;
    }

    private Post createPost(PostCreateRequest request, Category category, String content) {
        return switch (request.status()) {
            case DRAFT -> Post.createDraft(request.title(), request.summary(), content, category);
            case PUBLISHED -> Post.publish(request.title(), request.summary(), content, category);
        };
    }

    private void savePostTags(Post post, List<Tag> tags) {
        List<PostTag> postTags = tags.stream()
            .map(tag -> new PostTag(post, tag))
            .toList();

        postTagRepository.saveAll(postTags);
    }

    private void attachThumbnailImageIfPresent(Post post, Long thumbnailImageId) {
        if (thumbnailImageId == null) return;

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

    private void validateContentImageIds(String sanitizedContent, List<Long> contentImageIds) {
        Matcher matcher = CONTENT_IMAGE_PATTERN.matcher(sanitizedContent);
        Set<Long> referencedImageIds = new HashSet<>();

        while (matcher.find()) {
            referencedImageIds.add(Long.parseLong(matcher.group(1)));
        }

        if (!referencedImageIds.equals(new HashSet<>(contentImageIds))) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }
    }

    private void validateSanitizedContent(String content) {
        if (content == null || content.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_CONTENT);
        }
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
        if (contentImageIds.stream().anyMatch(Objects::isNull)) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }

        if (contentImageIds.size() != contentImageIds.stream().distinct().count()) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }

        if (thumbnailImageId != null && contentImageIds.contains(thumbnailImageId)) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE);
        }
    }
}
