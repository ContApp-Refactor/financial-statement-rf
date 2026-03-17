package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.repository;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IFinancialStatementRepository extends JpaRepository<FinancialStatementEntity, Long> {

    Optional<FinancialStatementEntity> findByReportId(UUID reportId);
}
