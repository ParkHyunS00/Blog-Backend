package com.parkhyuns00.blog.domain.category.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.domain.category.exception.CategoryException;
import com.parkhyuns00.blog.domain.category.exception.CategoryExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CategoryTest {

    @Test
    @DisplayName("카테고리를 생성하면 이름과 slug가 저장된다.")
    void test_create_category_success() {
        Category category = new Category("Spring", "spring");

        assertThat(category.getName()).isEqualTo("Spring");
        assertThat(category.getSlug()).isEqualTo("spring");
    }

    @Test
    @DisplayName("카테고리 이름은 앞뒤 공백을 제거해서 저장한다.")
    void test_trim_category_name() {
        Category category = new Category("  Spring  ", "spring");

        assertThat(category.getName()).isEqualTo("Spring");
        assertThat(category.getSlug()).isEqualTo("spring");
    }

    @Test
    @DisplayName("카테고리 이름이 null 이면 예외가 발생한다.")
    void test_create_fail_when_name_null() {
        assertThatThrownBy(() -> new Category(null, "spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("카테고리 이름이 공이면 예외가 발생한다.")
    void test_create_fail_when_name_blank() {
        assertThatThrownBy(() -> new Category("   ", "spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("카테고리 이름이 50자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_name_too_long() {
        String name = "a".repeat(51);

        assertThatThrownBy(() -> new Category(name, "spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("카테고리 slug가 null이면 예외가 발생한다.")
    void test_create_fail_when_slug_null() {
        assertThatThrownBy(() -> new Category("Spring", null))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("카테고리 slug가 공백이면 예외가 발생한다.")
    void test_create_fail_when_slug_blank() {
        assertThatThrownBy(() -> new Category("Spring", "   "))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }

    @Test
    @DisplayName("카테고리 slug가 80자를 초과하면 예외가 발생한다.")
    void test_create_fail_when_slug_too_long() {
        String slug = "a".repeat(81);

        assertThatThrownBy(() -> new Category("Spring", slug))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);
    }
}
