package com.parkhyuns00.blog.domain.post.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PostTest {

    @Test
    @DisplayName("초안 게시글을 생성하면 DRAFT 상태가 된다.")
    void test_create_draft_success() {
        Category category = new Category("Spring", "spring");

        Post post = Post.createDraft("title", "summary", "content", category);

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getSummary()).isEqualTo("summary");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("발행 게시글을 생성하면 PUBLISHED 상태가 된다.")
    void test_publish_post_success() {
        Category category = new Category("Spring", "spring");

        Post post = Post.publish("title", "summary", "content", category);

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getSummary()).isEqualTo("summary");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("게시글 제목과 요약은 앞뒤 공백을 제거해서 저장한다.")
    void test_trim_title_and_summary() {
        Category category = new Category("Spring", "spring");

        Post post = Post.createDraft("  title  ", "  summary  ", "content", category);

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getSummary()).isEqualTo("summary");
        assertThat(post.getContent()).isEqualTo("content");
    }

    @Test
    @DisplayName("게시글 제목이 null 이면 예외가 발생한다.")
    void test_create_fail_when_title_null() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft(null, "summary", "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_TITLE);
    }

    @Test
    @DisplayName("게시글 제목이 공백이면 예외가 발생한다.")
    void test_create_fail_when_title_blank() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft("   ", "summary", "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_TITLE);
    }

    @Test
    @DisplayName("게시글 제목이 200자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_title_too_long() {
        Category category = new Category("Spring", "spring");
        String title = "a".repeat(201);

        assertThatThrownBy(() -> Post.createDraft(title, "summary", "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_TITLE);
    }

    @Test
    @DisplayName("게시글 요약이 null 이면 예외가 발생한다.")
    void test_create_fail_when_summary_null() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft("title", null, "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_SUMMARY);
    }

    @Test
    @DisplayName("게시글 요약이 공이면 예외가 발생한다.")
    void test_create_fail_when_summary_blank() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft("title", "   ", "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_SUMMARY);
    }

    @Test
    @DisplayName("게시글 요약이 500자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_summary_too_long() {
        Category category = new Category("Spring", "spring");
        String summary = "a".repeat(501);

        assertThatThrownBy(() -> Post.createDraft("title", summary, "content", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_SUMMARY);
    }

    @Test
    @DisplayName("게시글 본문이 null이면 예외가 발생한다.")
    void test_create_fail_when_content_null() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft("title", "summary", null, category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CONTENT);
    }

    @Test
    @DisplayName("게시글 본문이 공백 이면 예외가 발생한다.")
    void test_create_fail_when_content_blank() {
        Category category = new Category("Spring", "spring");

        assertThatThrownBy(() -> Post.createDraft("title", "summary", "   ", category))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CONTENT);
    }

    @Test
    @DisplayName("게시글 카테고리가 null 이면 예외가 발생한다.")
    void test_create_fail_when_category_null() {
        assertThatThrownBy(() -> Post.createDraft("title", "summary", "content", null))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.INVALID_POST_CATEGORY);
    }

    @Test
    @DisplayName("초안 게시글을 발행하면 PUBLISHED 상태가 된다.")
    void test_publish_draft_post() {
        Category category = new Category("Spring", "spring");
        Post post = Post.createDraft("title", "summary", "content", category);

        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("이미 발행된 게시글을 다시 발행해도 PUBLISHED 상태가 유지된다.")
    void test_publish_already_published_post() {
        Category category = new Category("Spring", "spring");
        Post post = Post.publish("title", "summary", "content", category);

        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }
}
