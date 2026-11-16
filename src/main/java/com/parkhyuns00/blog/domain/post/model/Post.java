package com.parkhyuns00.blog.domain.post.model;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
public class Post extends BaseEntity {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "published_title", length = 200)
    private String publishedTitle;

    @Column(name = "published_content", columnDefinition = "LONGTEXT")
    private String publishedContent;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Post(String title, String summary, String content, PostStatus status, Category category) {
        if (status == PostStatus.PUBLISHED) {
            this.publishedTitle = title.trim();
            this.publishedContent = content;
        } else {
            this.title = title;
            this.content = content;
        }
        this.summary = summary.trim();
        this.status = status;
        this.category = category;
    }

    public static Post createDraft(String title, String summary, String content, Category category) {
        validateDraftLength(title, summary);

        return new Post(
            normalizeDraftValue(title),
            normalizeDraftSummary(summary),
            normalizeDraftValue(content),
            PostStatus.DRAFT,
            category
        );
    }

    public static Post publish(String title, String summary, String content, Category category) {
        validateForPublish(title, summary, content, category);

        return new Post(title.trim(), summary.trim(), content, PostStatus.PUBLISHED, category);
    }

    private static void validateForPublish(String title, String summary, String content, Category category) {
        validateTitle(title);
        validateSummary(summary);
        validateContent(content);
        validateCategory(category);
    }

    private static String normalizeDraftValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeDraftSummary(String value) {
        return value == null ? "" : value.trim();
    }

    public void publish() {
        if (this.status == PostStatus.PUBLISHED) return;

        validateForPublish(title, summary, content, category);

        this.publishedTitle = title;
        this.publishedContent = content;
        this.title = null;
        this.content = null;
        this.status = PostStatus.PUBLISHED;
    }

    public void updateDraft(String title, String summary, String content, Category category) {
        if (status != PostStatus.DRAFT) throw new PostException(PostExceptionCode.INVALID_POST_STATUS);

        validateDraftLength(title, summary);

        this.title = normalizeDraftValue(title);
        this.summary = normalizeDraftSummary(summary);
        this.content = normalizeDraftValue(content);
        this.category = category;
    }

    public void updatePublished(String title, String summary, String content, Category category) {
        if (status != PostStatus.PUBLISHED) throw new PostException(PostExceptionCode.INVALID_POST_STATUS);

        validateForPublish(title, summary, content, category);

        this.publishedTitle = title.trim();
        this.summary = summary.trim();
        this.publishedContent = content;
        this.category = category;
    }

    private static void validateDraftLength(String title, String summary) {
        if (title != null && title.trim().length() > MAX_TITLE_LENGTH) {
            throw new PostException(
                PostExceptionCode.INVALID_POST_TITLE
            );
        }

        if (summary != null && summary.trim().length() > MAX_SUMMARY_LENGTH) {
            throw new PostException(
                PostExceptionCode.INVALID_POST_SUMMARY
            );
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_TITLE);
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new PostException(PostExceptionCode.INVALID_POST_TITLE);
        }
    }

    private static void validateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_SUMMARY);
        }

        if (summary.trim().length() > MAX_SUMMARY_LENGTH) {
            throw new PostException(PostExceptionCode.INVALID_POST_SUMMARY);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_CONTENT);
        }
    }

    private static void validateCategory(Category category) {
        if (category == null) {
            throw new PostException(PostExceptionCode.INVALID_POST_CATEGORY);
        }
    }
}
