package com.parkhyuns00.blog.domain.visitor.model;

import com.parkhyuns00.blog.domain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(
    name = "visitor_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_visitor_records_date_visitor",
        columnNames = {"visit_date", "visitor_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitorRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false, updatable = false, length = 36)
    private String visitorId;

    @Column(name = "visit_date", nullable = false, updatable = false)
    private LocalDate visitDate;

    public VisitorRecord(UUID visitorId, LocalDate visitDate) {
        this.visitorId = Objects.requireNonNull(visitorId, "visitorId must not be null").toString();
        this.visitDate = Objects.requireNonNull(visitDate, "visitDate must not be null");
    }
}
