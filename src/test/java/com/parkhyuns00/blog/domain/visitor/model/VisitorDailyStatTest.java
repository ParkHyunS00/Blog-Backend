package com.parkhyuns00.blog.domain.visitor.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class VisitorDailyStatTest {

    @Test
    @DisplayName("일별 집계를 생성하면 집계 날짜와 방문자 수 0을 설정한다.")
    void test_create_visitor_daily_stats_success() {
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        VisitorDailyStat stat = new VisitorDailyStat(visitDate);

        assertThat(stat.getVisitDate()).isEqualTo(visitDate);
        assertThat(stat.getVisitorCount()).isZero();
    }

    @Test
    @DisplayName("집계 날짜가 없으면 일별 집계 생성에 실패한다.")
    void test_create_visitor_daily_stats_fail_when_visit_date_null() {
        assertThatThrownBy(() -> new VisitorDailyStat(null)).isInstanceOf(NullPointerException.class);
    }
}
