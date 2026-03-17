package com.unicauca.edu.co.financial_statements.domain.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementTemplate {
    private Long id;
    private String entId;
    private String name;
    private String pathLogotype;
    private String alignment;
    private String font;
    private Integer fontSize;
    private String mainColor;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
}
