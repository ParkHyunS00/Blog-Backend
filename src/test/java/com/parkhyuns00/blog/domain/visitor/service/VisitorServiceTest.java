package com.parkhyuns00.blog.domain.visitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkhyuns00.blog.domain.visitor.cache.VisitorDeduplicationCache;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorException;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorExceptionCode;
import com.parkhyuns00.blog.domain.visitor.repository.VisitorDailyStatRepository;
import com.parkhyuns00.blog.domain.visitor.repository.VisitorRecordRepository;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;

@Testcontainers
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
@ActiveProfiles("test")
public class VisitorServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private VisitorService visitorService;

    @MockitoSpyBean
    private VisitorRecordRepository visitorRecordRepository;

    @MockitoSpyBean
    private VisitorDeduplicationCache cache;

    @MockitoBean
    private Clock clock;

    @MockitoSpyBean
    private VisitorDailyStatRepository visitorDailyStatRepository;

    @BeforeEach
    void setUp() {
        visitorRecordRepository.deleteAllInBatch();
        visitorDailyStatRepository.deleteAllInBatch();
        clearInvocations(visitorRecordRepository, visitorDailyStatRepository);
        setTime(LocalDateTime.of(2026, 9, 12, 10, 0));
    }

    @Test
    @DisplayName("최초 방문이면 방문 기록과 일별 집계를 함께 저장한다.")
    void test_record_visit_success() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);

        visitorService.recordVisit(visitorId);

        assertThat(cache.hasVisited(visitedAt.toLocalDate(), visitorId)).isTrue();

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(
                visitedAt.toLocalDate(),
                visitorId.toString()
            )
        ).isTrue();

        assertThat(
            visitorDailyStatRepository.findById(visitedAt.toLocalDate())
                .orElseThrow()
                .getVisitorCount()
        ).isEqualTo(1L);
    }

    @Test
    @DisplayName("캐시에 방문 이력이 있으면 DB에 접근하지 않는다.")
    void test_record_visit_success_when_cached() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);
        visitorService.recordVisit(visitorId);
        clearInvocations(visitorRecordRepository, visitorDailyStatRepository);

        visitorService.recordVisit(visitorId);

        verifyNoInteractions(visitorRecordRepository, visitorDailyStatRepository);
        assertThat(cache.hasVisited(visitedAt.toLocalDate(), visitorId)).isTrue();
    }

    @Test
    @DisplayName("같은 날짜의 중복 방문이면 기록과 집계를 추가하지 않는다.")
    void test_record_visit_success_when_duplicate() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);

        visitorService.recordVisit(visitorId);

        setTime(visitedAt.plusHours(1));
        // 캐시가 퇴출된 상황에서도 DB 기록으로 중복을 방지한다.
        doReturn(false).when(cache).hasVisited(visitedAt.toLocalDate(), visitorId);
        clearInvocations(visitorDailyStatRepository, cache);
        visitorService.recordVisit(visitorId);
        verify(visitorDailyStatRepository, never()).incrementVisitorCount(any(), any());
        verify(cache).markVisited(visitedAt.toLocalDate(), visitorId);

        assertThat(visitorRecordRepository.count()).isEqualTo(1L);

        assertThat(
            visitorDailyStatRepository.findById(visitedAt.toLocalDate())
                .orElseThrow()
                .getVisitorCount()
        ).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 날짜에 다른 방문자가 방문하면 집계를 증가시킨다.")
    void test_record_visit_success_when_visitor_different() {
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);

        visitorService.recordVisit(UUID.randomUUID());
        setTime(visitedAt.plusHours(1));
        visitorService.recordVisit(UUID.randomUUID());

        assertThat(visitorRecordRepository.count()).isEqualTo(2L);

        assertThat(
            visitorDailyStatRepository.findById(visitedAt.toLocalDate())
                .orElseThrow()
                .getVisitorCount()
        ).isEqualTo(2L);
    }

    @Test
    @DisplayName("집계 증가에 실패하면 방문 기록도 롤백한다.")
    void test_record_visit_rollback_when_increment_failed() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);

        DataAccessResourceFailureException exception = new DataAccessResourceFailureException("집계 증가 실패");

        doThrow(exception)
            .when(visitorDailyStatRepository)
            .incrementVisitorCount(visitedAt.toLocalDate(), visitedAt);

        assertThatThrownBy(() -> visitorService.recordVisit(visitorId))
            .isInstanceOfSatisfying(VisitorException.class, e -> {
                assertThat(e.getExceptionCode()).isEqualTo(VisitorExceptionCode.VISITOR_RECORD_FAILED);
                assertThat(e.getCause()).isSameAs(exception);
            });

        assertThat(cache.hasVisited(visitedAt.toLocalDate(), visitorId)).isFalse();

        verify(visitorDailyStatRepository).incrementVisitorCount(visitedAt.toLocalDate(), visitedAt);

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(
                visitedAt.toLocalDate(),
                visitorId.toString()
            )
        ).isFalse();

        assertThat(visitorDailyStatRepository.findById(visitedAt.toLocalDate())).isEmpty();
    }

    @Test
    @DisplayName("같은 방문자가 다른 날짜에 방문하면 날짜별로 집계한다.")
    void test_record_visit_success_when_visit_date_different() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 23, 59);
        LocalDateTime nextVisitedAt = visitedAt.plusMinutes(1);

        setTime(visitedAt);
        visitorService.recordVisit(visitorId);
        setTime(nextVisitedAt);
        visitorService.recordVisit(visitorId);

        assertThat(visitorRecordRepository.count()).isEqualTo(2L);

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(
                visitedAt.toLocalDate(),
                visitorId.toString()
            )
        ).isTrue();

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(
                nextVisitedAt.toLocalDate(),
                visitorId.toString()
            )
        ).isTrue();

        assertThat(
            visitorDailyStatRepository.findById(visitedAt.toLocalDate())
                .orElseThrow()
                .getVisitorCount()
        ).isEqualTo(1L);

        assertThat(
            visitorDailyStatRepository.findById(nextVisitedAt.toLocalDate())
                .orElseThrow()
                .getVisitorCount()
        ).isEqualTo(1L);
    }

    @Test
    @DisplayName("방문자 ID가 없으면 비즈니스 예외를 반환한다.")
    void test_record_visit_fail_when_visitor_id_null() {
        assertThatThrownBy(() -> visitorService.recordVisit(null))
            .isInstanceOfSatisfying(VisitorException.class, e ->
                assertThat(e.getExceptionCode()).isEqualTo(VisitorExceptionCode.INVALID_VISITOR_ID)
            );

        verifyNoInteractions(visitorRecordRepository, visitorDailyStatRepository);
    }

    @Test
    @DisplayName("DB 커밋이 완료된 후 캐시에 방문을 등록한다.")
    void test_record_visit_mark_cache_after_commit() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);

        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(visitorRecordRepository.count()).isEqualTo(1L);
            assertThat(visitorDailyStatRepository.findById(visitedAt.toLocalDate())
                .orElseThrow().getVisitorCount()).isEqualTo(1L);
            return invocation.callRealMethod();
        }).when(cache).markVisited(visitedAt.toLocalDate(), visitorId);

        visitorService.recordVisit(visitorId);

        assertThat(cache.hasVisited(visitedAt.toLocalDate(), visitorId)).isTrue();
    }

    @Test
    @DisplayName("커밋 단계에서 실패해도 DB를 롤백하고 캐시에 등록하지 않는다.")
    void test_record_visit_rollback_when_commit_failed() {
        UUID visitorId = UUID.randomUUID();
        LocalDateTime visitedAt = LocalDateTime.of(2026, 9, 12, 10, 0);
        DataAccessResourceFailureException exception =
            new DataAccessResourceFailureException("커밋 직전 실패");

        doAnswer(invocation -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw exception;
                }
            });
            return false;
        }).when(visitorRecordRepository)
            .existsByVisitDateAndVisitorId(visitedAt.toLocalDate(), visitorId.toString());

        assertThatThrownBy(() -> visitorService.recordVisit(visitorId))
            .isInstanceOfSatisfying(VisitorException.class, e -> {
                assertThat(e.getExceptionCode()).isEqualTo(VisitorExceptionCode.VISITOR_RECORD_FAILED);
                assertThat(e.getCause()).isSameAs(exception);
            });

        assertThat(visitorRecordRepository.count()).isZero();
        assertThat(visitorDailyStatRepository.findById(visitedAt.toLocalDate())).isEmpty();
        assertThat(cache.hasVisited(visitedAt.toLocalDate(), visitorId)).isFalse();
    }

    @Test
    @DisplayName("전체와 오늘 및 어제 방문자 수를 반환한다.")
    void test_get_visitor_stats_success() {
        setTime(LocalDateTime.of(2026, 9, 12, 10, 0));
        visitorService.recordVisit(UUID.randomUUID());

        setTime(LocalDateTime.of(2026, 9, 13, 10, 0));
        visitorService.recordVisit(UUID.randomUUID());

        setTime(LocalDateTime.of(2026, 9, 14, 10, 0));
        visitorService.recordVisit(UUID.randomUUID());
        visitorService.recordVisit(UUID.randomUUID());

        VisitorStatsDto result = visitorService.getVisitorStats();

        assertThat(result.total()).isEqualTo(4L);
        assertThat(result.today()).isEqualTo(2L);
        assertThat(result.yesterday()).isEqualTo(1L);

        verify(visitorDailyStatRepository).findVisitorStats(
            LocalDate.of(2026, 9, 14),
            LocalDate.of(2026, 9, 13)
        );
    }

    @Test
    @DisplayName("방문 기록이 없으면 모든 방문자 수를 0으로 반환한다.")
    void test_get_visitor_stats_success_when_stats_empty() {
        setTime(LocalDateTime.of(2026, 9, 14, 10, 0));

        VisitorStatsDto result = visitorService.getVisitorStats();

        assertThat(result.total()).isZero();
        assertThat(result.today()).isZero();
        assertThat(result.yesterday()).isZero();
    }

    @Test
    @DisplayName("KST 기준으로 오늘과 어제 날짜를 계산한다.")
    void test_get_visitor_stats_success_with_seoul_date() {
        setTime(LocalDateTime.of(2026, 9, 14, 0, 30));

        visitorService.getVisitorStats();

        verify(visitorDailyStatRepository).findVisitorStats(
            LocalDate.of(2026, 9, 14),
            LocalDate.of(2026, 9, 13)
        );
    }

    private void setTime(LocalDateTime time) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        when(clock.getZone()).thenReturn(zone);
        when(clock.instant()).thenReturn(time.atZone(zone).toInstant());
    }
}
