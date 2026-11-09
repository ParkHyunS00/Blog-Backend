package com.parkhyuns00.blog.domain.visitor.model;

import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorException;
import com.parkhyuns00.blog.domain.visitor.exception.VisitorExceptionCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "visitor_daily_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitorDailyStat extends BaseEntity {

    @Id
    @Column(name = "visit_date", nullable = false, updatable = false)
    private LocalDate visitDate;

    @Column(name = "visitor_count", nullable = false)
    private long visitorCount = 0L;

    public VisitorDailyStat(LocalDate visitDate) {
        if (visitDate == null) {
            throw new VisitorException(VisitorExceptionCode.INVALID_VISIT_DATE);
        }

        this.visitDate = visitDate;
    }
}
