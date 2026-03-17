package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.repository;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEmailScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IFinancialStatementEmailScheduleRepository extends JpaRepository<FinancialStatementEmailScheduleEntity, Long> {

    List<FinancialStatementEmailScheduleEntity> findByFinancialStatement_ReportIdOrderByCreatedAtDesc(UUID reportId);

    List<FinancialStatementEmailScheduleEntity> findByActiveTrueAndNextRunAtLessThanEqual(OffsetDateTime cutoffAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FinancialStatementEmailScheduleEntity schedule
               set schedule.nextRunAt = :claimedUntil,
                   schedule.updatedAt = :updatedAt
             where schedule.id = :scheduleId
               and schedule.active = true
               and schedule.nextRunAt <= :cutoffAt
            """)
    int claimDueEmailSchedule(
            @Param("scheduleId") Long scheduleId,
            @Param("cutoffAt") OffsetDateTime cutoffAt,
            @Param("claimedUntil") OffsetDateTime claimedUntil,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
