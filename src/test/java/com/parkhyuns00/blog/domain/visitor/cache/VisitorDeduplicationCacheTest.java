package com.parkhyuns00.blog.domain.visitor.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

public class VisitorDeduplicationCacheTest {

    private final VisitorDeduplicationCache cache = new VisitorDeduplicationCache();

    @Test
    @DisplayName("캐시에 등록되지 않은 방문자는 방문 이력이 없다.")
    void test_has_visited_false_when_first_visit() {
        LocalDate visitDate = LocalDate.of(2026, 9, 13);
        UUID visitorId = UUID.randomUUID();

        boolean visited = cache.hasVisited(visitDate, visitorId);

        assertThat(visited).isFalse();
    }

    @Test
    @DisplayName("방문을 등록하면 같은 날짜의 방문 이력이 확인된다.")
    void test_has_visited_true_when_registered() {
        LocalDate visitDate = LocalDate.of(2026, 9, 13);
        UUID visitorId = UUID.randomUUID();

        cache.markVisited(visitDate, visitorId);

        boolean visited = cache.hasVisited(visitDate, visitorId);

        assertThat(visited).isTrue();
    }

    @Test
    @DisplayName("같은 방문자라도 날짜가 다르면 방문 이력이 없다.")
    void test_has_visited_false_when_visit_date_different() {
        LocalDate visitDate = LocalDate.of(2026, 9, 13);
        UUID visitorId = UUID.randomUUID();

        cache.markVisited(visitDate, visitorId);

        boolean visited = cache.hasVisited(visitDate.plusDays(1), visitorId);

        assertThat(visited).isFalse();
    }

    @Test
    @DisplayName("같은 날짜라도 방문자가 다르면 방문 이력이 없다.")
    void test_has_visited_false_when_visitor_different() {
        LocalDate visitDate = LocalDate.of(2026, 9, 13);
        UUID firstVisitorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondVisitorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        cache.markVisited(visitDate, firstVisitorId);

        boolean visited = cache.hasVisited(visitDate, secondVisitorId);

        assertThat(visited).isFalse();
    }
}
