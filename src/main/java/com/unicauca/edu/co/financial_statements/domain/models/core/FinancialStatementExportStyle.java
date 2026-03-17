package com.unicauca.edu.co.financial_statements.domain.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementExportStyle {
    private String pathLogotype;
    private String alignment;
    private String font;
    private Integer fontSize;
    private String mainColor;
}
