package com.parkhyuns00.blog.domain.post.model;

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
@Table(name = "post_images")
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostImageType type;

    @Column(name = "object_key", nullable = false, length = 500, unique = true)
    private String objectKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostImage(PostImageType type, String objectKey, String mimeType, int width, int height) {
        validateDimensions(width, height);

        this.type = type;
        this.objectKey = objectKey;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    public void detach() {
        this.post = null;
    }

    public boolean isAttachedTo(Post post) {
        return this.post == post;
    }

    public boolean isAttached() {
        return this.post != null;
    }

    public void attachTo(Post post) {
        if (post == null) {
            throw new PostException(PostExceptionCode.INVALID_POST);
        }

        if (isAttached()) {
            throw new PostException(PostExceptionCode.POST_IMAGE_ALREADY_ATTACHED);
        }

        this.post = post;
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new PostException(PostExceptionCode.INVALID_POST_IMAGE_DIMENSIONS);
        }
    }
}
