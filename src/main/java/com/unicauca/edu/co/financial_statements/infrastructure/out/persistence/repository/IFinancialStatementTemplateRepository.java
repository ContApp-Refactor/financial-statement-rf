package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.repository;

import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IFinancialStatementTemplateRepository extends JpaRepository<FinancialStatementTemplateEntity, Long> {

    Optional<FinancialStatementTemplateEntity> findByIdAndEntId(Long id, String entId);

    List<FinancialStatementTemplateEntity> findByEntIdOrderByIsDefaultDescCreatedAtAsc(String entId);

    long countByEntId(String entId);

    Optional<FinancialStatementTemplateEntity> findByEntIdAndIsDefaultTrue(String entId);
}
