package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.repository;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IFinancialStatementLogRepository extends JpaRepository<FinancialStatementLogEntity, Long> {

    List<FinancialStatementLogEntity> findByFinancialStatement_ReportIdOrderByCreatedAtDesc(UUID reportId);
}
