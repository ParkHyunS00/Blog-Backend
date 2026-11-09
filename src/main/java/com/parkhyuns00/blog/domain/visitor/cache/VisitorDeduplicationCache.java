package com.parkhyuns00.blog.domain.visitor.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class VisitorDeduplicationCache {

    private static final Duration EXPIRE_AFTER_WRITE = Duration.ofHours(24);
    private static final long MAXIMUM_SIZE = 100_000L;

    private final Cache<VisitorKey, Boolean> cache = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_AFTER_WRITE)
        .maximumSize(MAXIMUM_SIZE)
        .build();

    public boolean hasVisited(LocalDate visitDate, UUID visitorId) {
        return cache.getIfPresent(new VisitorKey(visitDate, visitorId)) != null;
    }

    public void markVisited(LocalDate visitDate, UUID visitorId) {
        cache.put(new VisitorKey(visitDate, visitorId), Boolean.TRUE);
    }

    private record VisitorKey(
        LocalDate visitDate,
        UUID visitorId
    ) {
    }
}
