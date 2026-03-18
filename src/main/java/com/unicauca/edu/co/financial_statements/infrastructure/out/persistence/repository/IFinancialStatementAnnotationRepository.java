package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.repository;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementAnnotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFinancialStatementAnnotationRepository extends JpaRepository<FinancialStatementAnnotationEntity, Long> {

    List<FinancialStatementAnnotationEntity> findByFinancialStatement_ReportIdOrderByCreatedAtAsc(UUID reportId);

    Optional<FinancialStatementAnnotationEntity> findByIdAndFinancialStatement_ReportId(Long annotationId, UUID reportId);

    void deleteByFinancialStatement_ReportId(UUID reportId);
}
