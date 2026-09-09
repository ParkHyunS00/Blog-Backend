package com.parkhyuns00.blog.domain.post.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class PostViewDeduplicationCache {

    private static final Duration EXPIRE_AFTER_WRITE = Duration.ofHours(24);
    private static final long MAXIMUM_SIZE = 100_000L;
    private final Cache<PostViewKey, Boolean> cache = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_AFTER_WRITE)
        .maximumSize(MAXIMUM_SIZE)
        .build();

    public boolean reserve(Long postId, UUID visitorId) {
        PostViewKey key = new PostViewKey(postId, visitorId);
        return cache.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    public void release(Long postId, UUID visitorId) {
        cache.invalidate(new PostViewKey(postId, visitorId));
    }

    private record PostViewKey(
        Long postId,
        UUID visitorId
    ) {
    }
}
