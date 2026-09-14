package com.parkhyuns00.blog.domain.visitor.repository;

import com.parkhyuns00.blog.domain.visitor.model.VisitorRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface VisitorRecordRepository extends JpaRepository<VisitorRecord, Long> {

    boolean existsByVisitDateAndVisitorId(LocalDate visitDate, String visitorId);
}
