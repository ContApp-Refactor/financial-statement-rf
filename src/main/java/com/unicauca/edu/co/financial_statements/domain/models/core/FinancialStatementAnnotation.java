package com.unicauca.edu.co.financial_statements.domain.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementAnnotation {
    private Long id;
    private UUID reportId;
    private String text;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
