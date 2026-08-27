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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Post(String title, String summary, String content, PostStatus status, Category category) {
        this.title = title.trim();
        this.summary = summary.trim();
        this.content = content;
        this.status = status;
        this.category = category;
    }

    public static Post createDraft(String title, String summary, String content, Category category) {
        validateDraftLength(title, summary);

        return new Post(
            normalizeDraftValue(title),
            normalizeDraftValue(summary),
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
        return value == null ? "" : value.trim();
    }

    public void publish() {
        if (this.status == PostStatus.PUBLISHED) return;

        validateForPublish(title, summary, content, category);

        this.status = PostStatus.PUBLISHED;
    }

    public void updateDraft(String title, String summary, String content, Category category) {
        if (status != PostStatus.DRAFT) throw new PostException(PostExceptionCode.INVALID_POST_STATUS);

        validateDraftLength(title, summary);

        this.title = normalizeDraftValue(title);
        this.summary = normalizeDraftValue(summary);
        this.content = normalizeDraftValue(content);
        this.category = category;
    }

    public void updatePublished(String title, String summary, String content, Category category) {
        if (status != PostStatus.PUBLISHED) throw new PostException(PostExceptionCode.INVALID_POST_STATUS);

        validateForPublish(title, summary, content, category);

        this.title = title.trim();
        this.summary = summary.trim();
        this.content = content;
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
