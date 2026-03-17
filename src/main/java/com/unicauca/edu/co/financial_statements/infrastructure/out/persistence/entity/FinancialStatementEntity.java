package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "FINANCIAL_STATEMENT",
        indexes = {
                @Index(name = "idx_financial_statement_report_id", columnList = "reportId", unique = true),
                @Index(name = "idx_financial_statement_ent_id", columnList = "entId")
        }
)
public class FinancialStatementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EFinancialStatementType type;

    @Column(nullable = false)
    private String entId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String reportSnapshot;
}
