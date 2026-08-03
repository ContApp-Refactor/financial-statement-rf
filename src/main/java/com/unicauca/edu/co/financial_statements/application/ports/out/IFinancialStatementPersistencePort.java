package com.unicauca.edu.co.financial_statements.application.ports.out;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementAnnotationEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementHistoryEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementLogEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    void deleteTemplate(FinancialStatementTemplateEntity entity);

    FinancialStatementAnnotationEntity saveAnnotation(FinancialStatementAnnotationEntity entity);

    Optional<FinancialStatementAnnotationEntity> findAnnotationByIdAndReportId(Long annotationId, UUID reportId);

    List<FinancialStatementAnnotationEntity> findAnnotationsByReportId(UUID reportId);

    void deleteAnnotation(FinancialStatementAnnotationEntity entity);
}
