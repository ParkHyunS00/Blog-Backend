package com.parkhyuns00.blog.domain.post.model;

import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "post_tags",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_tags_post_tag",
            columnNames = {"post_id", "tag_id"}
        )
    }
)
public class PostTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public PostTag(Post post, Tag tag) {
        validatePost(post);
        validateTag(tag);

        this.post = post;
        this.tag = tag;
    }

    private void validatePost(Post post) {
        if (post == null) {
            throw new PostException(PostExceptionCode.INVALID_POST);
        }
    }

    private void validateTag(Tag tag) {
        if (tag == null) {
            throw new PostException(PostExceptionCode.INVALID_POST_TAG);
        }
    }
}
