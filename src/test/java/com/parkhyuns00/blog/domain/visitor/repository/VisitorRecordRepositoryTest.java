package com.parkhyuns00.blog.domain.visitor.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.visitor.model.VisitorRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

@ActiveProfiles("test")
@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
public class VisitorRecordRepositoryTest {

    @Autowired
    private VisitorRecordRepository visitorRecordRepository;

    @Test
    @DisplayName("방문 기록을 저장하면 ID와 생성 및 수정 시각이 설정된다.")
    void test_save_visitor_record_success() {
        UUID visitorId = UUID.randomUUID();
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        VisitorRecord saved = visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVisitorId()).isEqualTo(visitorId.toString());
        assertThat(saved.getVisitDate()).isEqualTo(visitDate);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 방문자의 같은 날짜 기록은 중복 저장할 수 없다.")
    void test_save_visitor_record_fail_when_duplicate() {
        UUID visitorId = UUID.randomUUID();
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate));

        assertThatThrownBy(() -> visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 방문자도 날짜가 다르면 기록을 저장한다.")
    void test_save_visitor_record_success_when_date_different() {
        UUID visitorId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 9, 12);

        visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, today));
        visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, today.plusDays(1)));

        assertThat(visitorRecordRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 날짜에도 방문자가 다르면 기록을 저장한다.")
    void test_save_visitor_record_success_when_visitor_different() {
        LocalDate today = LocalDate.of(2026, 9, 12);

        visitorRecordRepository.saveAndFlush(
            new VisitorRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                today
            )
        );
        visitorRecordRepository.saveAndFlush(
            new VisitorRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                today
            )
        );

        assertThat(visitorRecordRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 날짜와 방문자 ID의 기록이 있으면 true를 반환한다.")
    void test_exists_by_visit_date_and_visitor_id_success() {
        UUID visitorId = UUID.randomUUID();
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate));

        boolean result = visitorRecordRepository.existsByVisitDateAndVisitorId(visitDate, visitorId.toString());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("날짜 또는 방문자 ID가 다르면 false를 반환한다.")
    void test_exists_by_visit_date_and_visitor_id_false_when_not_matching() {
        UUID visitorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID otherVisitorId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        LocalDate visitDate = LocalDate.of(2026, 9, 12);

        visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate));

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(
                visitDate.plusDays(1), visitorId.toString()
            )
        ).isFalse();

        assertThat(
            visitorRecordRepository.existsByVisitDateAndVisitorId(visitDate, otherVisitorId.toString())
        ).isFalse();
    }
}
