package com.parkhyuns00.blog.domain.visitor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.visitor.model.VisitorDailyStat;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Testcontainers
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
@ActiveProfiles("test")
@Transactional
public class VisitorDailyStatRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private VisitorDailyStatRepository visitorDailyStatRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("해당 날짜의 집계가 없으면 방문자 수 1로 생성한다.")
    void test_increment_visitor_count_success_when_stat_not_exists() {
        LocalDate visitDate = LocalDate.of(2026, 9, 12);
        LocalDateTime recordedAt = visitDate.atTime(10, 0);

        visitorDailyStatRepository.incrementVisitorCount(visitDate, recordedAt);

        entityManager.clear();

        VisitorDailyStat result = visitorDailyStatRepository
            .findById(visitDate)
            .orElseThrow();

        assertThat(result.getVisitDate()).isEqualTo(visitDate);
        assertThat(result.getVisitorCount()).isEqualTo(1L);
        assertThat(result.getCreatedAt()).isEqualTo(recordedAt);
        assertThat(result.getUpdatedAt()).isEqualTo(recordedAt);
    }

    @Test
    @DisplayName("기존 집계는 방문자 수를 증가시키고 수정 시각만 변경한다.")
    void test_increment_visitor_count_success_when_stat_exists() {
        LocalDate visitDate = LocalDate.of(2026, 9, 12);
        LocalDateTime firstVisitAt = visitDate.atTime(10, 0);
        LocalDateTime secondVisitAt = visitDate.atTime(11, 0);

        visitorDailyStatRepository.incrementVisitorCount(visitDate, firstVisitAt);
        visitorDailyStatRepository.incrementVisitorCount(visitDate, secondVisitAt);

        entityManager.clear();

        VisitorDailyStat result = visitorDailyStatRepository
            .findById(visitDate)
            .orElseThrow();

        assertThat(visitorDailyStatRepository.count()).isEqualTo(1L);
        assertThat(result.getVisitorCount()).isEqualTo(2L);
        assertThat(result.getCreatedAt()).isEqualTo(firstVisitAt);
        assertThat(result.getUpdatedAt()).isEqualTo(secondVisitAt);
    }

    @Test
    @DisplayName("방문자 수를 증가시켜도 다른 날짜의 집계는 변경하지 않는다.")
    void test_increment_visitor_count_success_without_affecting_other_dates() {
        LocalDate today = LocalDate.of(2026, 9, 12);
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayVisitAt = yesterday.atTime(10, 0);

        visitorDailyStatRepository.incrementVisitorCount(yesterday, yesterdayVisitAt);
        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(10, 0));
        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(11, 0));

        entityManager.clear();

        VisitorDailyStat todayStat = visitorDailyStatRepository
            .findById(today)
            .orElseThrow();

        VisitorDailyStat yesterdayStat = visitorDailyStatRepository
            .findById(yesterday)
            .orElseThrow();

        assertThat(visitorDailyStatRepository.count()).isEqualTo(2L);
        assertThat(todayStat.getVisitorCount()).isEqualTo(2L);
        assertThat(yesterdayStat.getVisitorCount()).isEqualTo(1L);
        assertThat(yesterdayStat.getCreatedAt()).isEqualTo(yesterdayVisitAt);
        assertThat(yesterdayStat.getUpdatedAt()).isEqualTo(yesterdayVisitAt);
    }

    @Test
    @DisplayName("전체와 오늘 및 어제 방문자 수를 조회한다.")
    void test_find_visitor_stats_success() {
        LocalDate today = LocalDate.of(2026, 9, 14);
        LocalDate yesterday = today.minusDays(1);
        LocalDate previousDate = today.minusDays(2);

        visitorDailyStatRepository.incrementVisitorCount(previousDate, previousDate.atTime(10, 0));
        visitorDailyStatRepository.incrementVisitorCount(yesterday, yesterday.atTime(10, 0));
        visitorDailyStatRepository.incrementVisitorCount(yesterday, yesterday.atTime(11, 0));
        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(10, 0));
        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(11, 0));
        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(12, 0));

        VisitorStatsDto result = visitorDailyStatRepository.findVisitorStats(today, yesterday);

        assertThat(result.total()).isEqualTo(6L);
        assertThat(result.today()).isEqualTo(3L);
        assertThat(result.yesterday()).isEqualTo(2L);
    }

    @Test
    @DisplayName("방문자 집계가 없으면 모든 방문자 수를 0으로 반환한다.")
    void test_find_visitor_stats_success_when_stats_empty() {
        LocalDate today = LocalDate.of(2026, 9, 14);

        VisitorStatsDto result = visitorDailyStatRepository.findVisitorStats(today, today.minusDays(1));

        assertThat(result.total()).isZero();
        assertThat(result.today()).isZero();
        assertThat(result.yesterday()).isZero();
    }

    @Test
    @DisplayName("오늘 집계가 없으면 오늘 방문자 수만 0으로 반환한다.")
    void test_find_visitor_stats_success_when_today_stat_missing() {
        LocalDate today = LocalDate.of(2026, 9, 14);
        LocalDate yesterday = today.minusDays(1);

        visitorDailyStatRepository.incrementVisitorCount(yesterday, yesterday.atTime(10, 0));

        VisitorStatsDto result = visitorDailyStatRepository.findVisitorStats(today, yesterday);

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.today()).isZero();
        assertThat(result.yesterday()).isEqualTo(1L);
    }

    @Test
    @DisplayName("어제 집계가 없으면 어제 방문자 수만 0으로 반환한다.")
    void test_find_visitor_stats_success_when_yesterday_stat_missing() {
        LocalDate today = LocalDate.of(2026, 9, 14);

        visitorDailyStatRepository.incrementVisitorCount(today, today.atTime(10, 0));

        VisitorStatsDto result = visitorDailyStatRepository.findVisitorStats(today, today.minusDays(1));

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.today()).isEqualTo(1L);
        assertThat(result.yesterday()).isZero();
    }

    @Test
    @DisplayName("과거 집계만 있어도 전체 방문자 수에 포함한다.")
    void test_find_visitor_stats_success_when_only_past_stats_exist() {
        LocalDate today = LocalDate.of(2026, 9, 14);
        LocalDate previousDate = today.minusDays(7);

        visitorDailyStatRepository.incrementVisitorCount(previousDate, previousDate.atTime(10, 0));

        VisitorStatsDto result = visitorDailyStatRepository.findVisitorStats(today, today.minusDays(1));

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.today()).isZero();
        assertThat(result.yesterday()).isZero();
    }
}
