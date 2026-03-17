package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "FINANCIAL_STATEMENT_LOG",
        indexes = {
                @Index(name = "idx_financial_statement_log_created_at", columnList = "createdAt"),
                @Index(name = "idx_financial_statement_log_statement_id", columnList = "financial_statement_id")
        }
)
public class FinancialStatementLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 80)
    private String icon;

    @Column(length = 30)
    private String color;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_statement_id", nullable = false)
    private FinancialStatementEntity financialStatement;
}
