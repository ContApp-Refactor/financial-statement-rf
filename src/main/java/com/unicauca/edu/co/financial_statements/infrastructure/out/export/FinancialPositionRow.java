package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import java.math.BigDecimal;

record FinancialPositionRow(
        String lineDescription,
        String note,
        BigDecimal currentAmount,
        BigDecimal currentPercentage,
        BigDecimal previousAmount,
        BigDecimal previousPercentage,
        BigDecimal variation,
        BigDecimal variationPercentage,
        String rowType
) {
}
