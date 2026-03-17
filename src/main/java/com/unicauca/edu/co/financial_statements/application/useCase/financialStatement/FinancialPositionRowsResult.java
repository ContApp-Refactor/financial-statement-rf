package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record FinancialPositionRowsResult(
        List<Map<String, Object>> rows,
        BigDecimal totalAssets,
        BigDecimal previousTotalAssets,
        BigDecimal totalLiabilities,
        BigDecimal previousTotalLiabilities,
        BigDecimal totalEquity,
        BigDecimal previousTotalEquity
) {
}
