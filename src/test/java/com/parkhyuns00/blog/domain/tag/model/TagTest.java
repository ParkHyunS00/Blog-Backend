package com.parkhyuns00.blog.domain.tag.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.domain.tag.exception.TagException;
import com.parkhyuns00.blog.domain.tag.exception.TagExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    @DisplayName("태그를 생성하면 이름과 slug가 저장된다.")
    void test_create_tag_success() {
        Tag tag = new Tag("Java", "java");

        assertThat(tag.getName()).isEqualTo("Java");
        assertThat(tag.getSlug()).isEqualTo("java");
    }

    @Test
    @DisplayName("태그 이름은 앞뒤 공백을 제거해서 저장한다.")
    void test_trim_tag_name() {
        Tag tag = new Tag("  Java  ", "java");

        assertThat(tag.getName()).isEqualTo("Java");
        assertThat(tag.getSlug()).isEqualTo("java");
    }

    @Test
    @DisplayName("태그 이름이 null이면 예외가 발생한다.")
    void test_create_fail_when_name_null() {
        assertThatThrownBy(() -> new Tag(null, "java"))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }

    @Test
    @DisplayName("태그 이름이 공이면 예외가 발생한다.")
    void test_create_fail_when_name_blank() {
        assertThatThrownBy(() -> new Tag("   ", "java"))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }

    @Test
    @DisplayName("태그 이름이 20자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_name_too_long() {
        String name = "a".repeat(21);

        assertThatThrownBy(() -> new Tag(name, "java"))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }

    @Test
    @DisplayName("태그 slug가 null이면 예외가 발생한다.")
    void test_create_fail_when_slug_null() {
        assertThatThrownBy(() -> new Tag("Java", null))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }

    @Test
    @DisplayName("태그 slug가 공백이면 예외가 발생한다.")
    void test_create_fail_when_slug_blank() {
        assertThatThrownBy(() -> new Tag("Java", "   "))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }

    @Test
    @DisplayName("태그 slug가 80자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_slug_too_long() {
        String slug = "a".repeat(81);

        assertThatThrownBy(() -> new Tag("Java", slug))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);
    }
}
