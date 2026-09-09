package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.post.cache.PostViewDeduplicationCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class PostViewDeduplicationCacheTest {

    private final PostViewDeduplicationCache cache = new PostViewDeduplicationCache();

    @Test
    @DisplayName("게시글과 방문자 조합이 처음 등록되면 조회수 증가를 허용한다.")
    void test_reserve_success_when_first_view() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();

        boolean reserved = cache.reserve(postId, visitorId);

        assertThat(reserved).isTrue();
    }

    @Test
    @DisplayName("동일한 방문자의 동일 게시글 중복 조회는 허용하지 않는다.")
    void test_reserve_fail_when_duplicate_view() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();

        cache.reserve(postId, visitorId);

        boolean reserved = cache.reserve(postId, visitorId);

        assertThat(reserved).isFalse();
    }

    @Test
    @DisplayName("동일한 방문자라도 다른 게시글 조회는 허용한다.")
    void test_reserve_success_when_post_different() {
        UUID visitorId = UUID.randomUUID();

        cache.reserve(1L, visitorId);

        boolean reserved = cache.reserve(2L, visitorId);

        assertThat(reserved).isTrue();
    }

    @Test
    @DisplayName("동일한 게시글이라도 다른 방문자의 조회는 허용한다.")
    void test_reserve_success_when_visitor_different() {
        Long postId = 1L;

        cache.reserve(postId, UUID.randomUUID());

        boolean reserved = cache.reserve(postId, UUID.randomUUID());

        assertThat(reserved).isTrue();
    }

    @Test
    @DisplayName("예약을 해제하면 동일한 게시글과 방문자를 다시 예약할 수 있다.")
    void test_reserve_success_after_release() {
        Long postId = 1L;
        UUID visitorId = UUID.randomUUID();

        cache.reserve(postId, visitorId);

        cache.release(postId, visitorId);

        boolean reserved = cache.reserve(postId, visitorId);

        assertThat(reserved).isTrue();
    }
}
