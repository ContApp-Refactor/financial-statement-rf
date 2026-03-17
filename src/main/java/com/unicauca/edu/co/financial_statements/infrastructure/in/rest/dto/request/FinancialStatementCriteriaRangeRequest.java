package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementCriteriaRangeRequest {
    private Long from;
    private Long to;
}
