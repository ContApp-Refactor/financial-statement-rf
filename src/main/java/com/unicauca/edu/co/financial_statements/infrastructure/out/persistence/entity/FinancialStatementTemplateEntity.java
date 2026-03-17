package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "FINANCIAL_STATEMENT_TEMPLATE",
        indexes = {
                @Index(name = "idx_financial_statement_template_ent_id", columnList = "entId"),
                @Index(name = "idx_financial_statement_template_default", columnList = "entId,isDefault")
        }
)
public class FinancialStatementTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entId;

    @Column(nullable = false)
    private String name;

    @Column
    private String pathLogotype;

    @Column
    private String alignment;

    @Column
    private String font;

    @Column
    private Integer fontSize;

    @Column
    private String mainColor;

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
