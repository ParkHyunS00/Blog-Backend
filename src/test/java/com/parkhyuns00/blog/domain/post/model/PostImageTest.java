package com.parkhyuns00.blog.domain.post.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PostImageTest {

    @Test
    @DisplayName("게시글에 연결된 이미지를 분리한다.")
    void test_detach_post_image_success() {
        Category category = new Category("Spring", "spring");
        Post post = Post.createDraft("title", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        image.attachTo(post);

        assertThat(image.getPost()).isSameAs(post);

        image.detach();

        assertThat(image.getPost()).isNull();
        assertThat(image.isAttached()).isFalse();
    }

    @Test
    @DisplayName("연결되지 않은 이미지를 분리해도 연결되지 않은 상태를 유지한다.")
    void test_detach_unattached_post_image() {
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        image.detach();

        assertThat(image.getPost()).isNull();
        assertThat(image.isAttached()).isFalse();
    }

    @Test
    @DisplayName("이미지가 지정한 게시글에 연결되어 있는지 확인한다.")
    void test_post_image_attached_to_post() {
        Category category = new Category("Spring", "spring");
        Post post = Post.createDraft("title", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        image.attachTo(post);

        assertThat(image.isAttachedTo(post)).isTrue();
    }

    @Test
    @DisplayName("이미지가 다른 게시글에 연결되어 있으면 false를 반환한다.")
    void test_post_image_not_attached_to_other_post() {
        Category category = new Category("Spring", "spring");
        Post attachedPost = Post.createDraft("attached", "summary", "content", category);
        Post otherPost = Post.createDraft("other", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png", 1200, 630);

        image.attachTo(attachedPost);

        assertThat(image.isAttachedTo(otherPost)).isFalse();
    }

    @Test
    @DisplayName("이미지 크기 정보를 포함하여 게시글 이미지를 생성한다.")
    void test_create_post_image_with_dimensions_success() {
        PostImage image = new PostImage(
            PostImageType.CONTENT,
            "posts/content/test.webp",
            "image/webp",
            1200,
            630
        );

        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(630);
    }

    @Test
    @DisplayName("이미지 너비가 0 이하면 예외가 발생한다.")
    void test_create_post_image_fail_when_width_not_positive() {
        assertThatThrownBy(() -> new PostImage(
            PostImageType.CONTENT,
            "posts/content/test.webp",
            "image/webp",
            0,
            630)
        )
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_IMAGE_DIMENSIONS);
    }
}
