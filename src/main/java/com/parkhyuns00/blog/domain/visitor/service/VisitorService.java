package com.parkhyuns00.blog.domain.visitor.service;

import com.parkhyuns00.blog.domain.visitor.cache.VisitorDeduplicationCache;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorException;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorExceptionCode;
import com.parkhyuns00.blog.domain.visitor.model.VisitorRecord;
import com.parkhyuns00.blog.domain.visitor.repository.VisitorDailyStatRepository;
import com.parkhyuns00.blog.domain.visitor.repository.VisitorRecordRepository;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRecordRepository visitorRecordRepository;
    private final VisitorDailyStatRepository visitorDailyStatRepository;
    private final VisitorDeduplicationCache cache;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    // DB 커밋 후 캐시 갱신 보장
    public void recordVisit(UUID visitorId) {
        if (visitorId == null) {
            throw new VisitorException(VisitorExceptionCode.INVALID_VISITOR_ID);
        }

        LocalDateTime visitedAt = LocalDateTime.now(clock);
        LocalDate visitDate = visitedAt.toLocalDate();

        if (cache.hasVisited(visitDate, visitorId)) {
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(_ -> {
                if (visitorRecordRepository.existsByVisitDateAndVisitorId(visitDate, visitorId.toString())) {
                    return;
                }

                visitorRecordRepository.saveAndFlush(new VisitorRecord(visitorId, visitDate));
                visitorDailyStatRepository.incrementVisitorCount(visitDate, visitedAt);
            });
        } catch (DataAccessException | TransactionException exception) {
            throw new VisitorException(VisitorExceptionCode.VISITOR_RECORD_FAILED, exception);
        }

        cache.markVisited(visitDate, visitorId);
    }

    @Transactional(readOnly = true)
    public VisitorStatsDto getVisitorStats() {
        LocalDate today = LocalDate.now(clock);
        LocalDate yesterday = today.minusDays(1);

        return visitorDailyStatRepository.findVisitorStats(today, yesterday);
    }
}
