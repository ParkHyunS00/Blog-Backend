package com.parkhyuns00.blog.domain.post.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.category.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PostImageTest {

    @Test
    @DisplayName("게시글에 연결된 이미지를 분리한다.")
    void test_detach_post_image_success() {
        Category category = new Category("Spring", "spring");
        Post post = Post.createDraft("title", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png");

        image.attachTo(post);

        assertThat(image.getPost()).isSameAs(post);

        image.detach();

        assertThat(image.getPost()).isNull();
        assertThat(image.isAttached()).isFalse();
    }

    @Test
    @DisplayName("연결되지 않은 이미지를 분리해도 연결되지 않은 상태를 유지한다.")
    void test_detach_unattached_post_image() {
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png");

        image.detach();

        assertThat(image.getPost()).isNull();
        assertThat(image.isAttached()).isFalse();
    }

    @Test
    @DisplayName("이미지가 지정한 게시글에 연결되어 있는지 확인한다.")
    void test_post_image_attached_to_post() {
        Category category = new Category("Spring", "spring");
        Post post = Post.createDraft("title", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png");

        image.attachTo(post);

        assertThat(image.isAttachedTo(post)).isTrue();
    }

    @Test
    @DisplayName("이미지가 다른 게시글에 연결되어 있으면 false를 반환한다.")
    void test_post_image_not_attached_to_other_post() {
        Category category = new Category("Spring", "spring");
        Post attachedPost = Post.createDraft("attached", "summary", "content", category);
        Post otherPost = Post.createDraft("other", "summary", "content", category);
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/test.png", "image/png");

        image.attachTo(attachedPost);

        assertThat(image.isAttachedTo(otherPost)).isFalse();
    }
}
