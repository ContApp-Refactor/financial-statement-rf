package com.unicauca.edu.co.financial_statements.domain.models.core;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementRequest {
    private String entId;
    private EFinancialStatementType type;
    private FinancialStatementCriteria criteria;
}
