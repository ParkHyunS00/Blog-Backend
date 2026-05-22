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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private Post(String title, String summary, String content, PostStatus status, Category category) {
        validateTitle(title);
        validateSummary(summary);
        validateContent(content);
        validateCategory(category);

        this.title = title.trim();
        this.summary = summary.trim();
        this.content = content;
        this.status = status;
        this.category = category;
    }

    public static Post createDraft(String title, String summary, String content, Category category) {
        return new Post(title, summary, content, PostStatus.DRAFT, category);
    }

    public static Post publish(String title, String summary, String content, Category category) {
        return new Post(title, summary, content, PostStatus.PUBLISHED, category);
    }

    public void publish() {
        if (this.status == PostStatus.PUBLISHED) {
            return;
        }

        this.status = PostStatus.PUBLISHED;
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_TITLE);
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new PostException(PostExceptionCode.INVALID_POST_TITLE);
        }
    }

    private void validateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_SUMMARY);
        }

        if (summary.trim().length() > MAX_SUMMARY_LENGTH) {
            throw new PostException(PostExceptionCode.INVALID_POST_SUMMARY);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new PostException(PostExceptionCode.INVALID_POST_CONTENT);
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new PostException(PostExceptionCode.INVALID_POST_CATEGORY);
        }
    }
}
