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
        name = "FINANCIAL_STATEMENT_HISTORY",
        indexes = {
                @Index(name = "idx_financial_statement_history_created_at", columnList = "createdAt"),
                @Index(name = "idx_financial_statement_history_statement_id", columnList = "financial_statement_id")
        }
)
public class FinancialStatementHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String state;

    @Column(nullable = false, length = 40)
    private String deliveryWay;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_statement_id", nullable = false)
    private FinancialStatementEntity financialStatement;
}
