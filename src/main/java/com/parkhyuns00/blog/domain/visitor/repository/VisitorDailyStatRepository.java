package com.parkhyuns00.blog.domain.visitor.repository;

import com.parkhyuns00.blog.domain.visitor.model.VisitorDailyStat;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface VisitorDailyStatRepository extends JpaRepository<VisitorDailyStat, LocalDate> {

    @Modifying
    @Query(value = """
          insert into visitor_daily_stats (
              visit_date,
              visitor_count,
              created_at,
              updated_at
          )
          values (:visitDate, 1, :recordedAt, :recordedAt)
          on DUPLICATE key update
              visitor_count = visitor_count + 1,
              updated_at = :recordedAt
          """,
        nativeQuery = true
    )
    void incrementVisitorCount(@Param("visitDate") LocalDate visitDate, @Param("recordedAt") LocalDateTime recordedAt);

    @Query("""
        select new com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto(
            coalesce(sum(vds.visitorCount), 0L),
            coalesce(sum(case when vds.visitDate = :today then vds.visitorCount else 0L end), 0L),
            coalesce(sum(case when vds.visitDate = :yesterday then vds.visitorCount else 0L end), 0L))
            from VisitorDailyStat vds
        """
    )
    VisitorStatsDto findVisitorStats(@Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);
}
