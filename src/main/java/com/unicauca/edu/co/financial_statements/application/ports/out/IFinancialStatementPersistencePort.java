package com.unicauca.edu.co.financial_statements.application.ports.out;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEmailScheduleEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementHistoryEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementLogEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFinancialStatementPersistencePort {

    FinancialStatementEntity saveFinancialStatement(FinancialStatementEntity entity);

    Optional<FinancialStatementEntity> findFinancialStatementByReportId(UUID reportId);

    FinancialStatementHistoryEntity saveHistory(FinancialStatementHistoryEntity entity);

    Page<FinancialStatementHistoryEntity> findHistoryByEnterprise(String entId, Pageable pageable);

    FinancialStatementLogEntity saveLog(FinancialStatementLogEntity entity);

    List<FinancialStatementLogEntity> findLogsByReportId(UUID reportId);

    FinancialStatementTemplateEntity saveTemplate(FinancialStatementTemplateEntity entity);

    Optional<FinancialStatementTemplateEntity> findTemplateByIdAndEnterprise(Long id, String entId);

    List<FinancialStatementTemplateEntity> findTemplatesByEnterprise(String entId);

    long countTemplatesByEnterprise(String entId);

    Optional<FinancialStatementTemplateEntity> findDefaultTemplateByEnterprise(String entId);

    FinancialStatementEmailScheduleEntity saveEmailSchedule(FinancialStatementEmailScheduleEntity entity);

    Optional<FinancialStatementEmailScheduleEntity> findEmailScheduleById(Long scheduleId);

    List<FinancialStatementEmailScheduleEntity> findEmailSchedulesByReportId(UUID reportId);

    List<FinancialStatementEmailScheduleEntity> findDueActiveEmailSchedules(OffsetDateTime cutoffAt);

    boolean claimDueEmailSchedule(
            Long scheduleId,
            OffsetDateTime cutoffAt,
            OffsetDateTime claimedUntil,
            OffsetDateTime updatedAt
    );
}
