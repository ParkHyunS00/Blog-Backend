package com.parkhyuns00.blog.domain.visitor.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

public class VisitorRecordTest {

    @Test
    @DisplayName("방문 기록을 생성하면 방문자 ID와 집계 날짜를 설정한다.")
    void test_create_visitor_record_success() {
        UUID visitorId = UUID.randomUUID();
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        VisitorRecord record = new VisitorRecord(visitorId, visitDate);

        assertThat(record.getVisitorId()).isEqualTo(visitorId.toString());
        assertThat(record.getVisitDate()).isEqualTo(visitDate);
    }

    @Test
    @DisplayName("방문자 ID가 없으면 방문 기록 생성에 실패한다.")
    void test_create_visitor_record_fail_when_visitor_id_null() {
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        assertThatThrownBy(() -> new VisitorRecord(null, visitDate)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("집계 날짜가 없으면 방문 기록 생성에 실패한다.")
    void test_create_visitor_record_fail_when_visit_date_null() {
        UUID visitorId = UUID.randomUUID();

        assertThatThrownBy(() -> new VisitorRecord(visitorId, null)).isInstanceOf(NullPointerException.class);
    }
}
