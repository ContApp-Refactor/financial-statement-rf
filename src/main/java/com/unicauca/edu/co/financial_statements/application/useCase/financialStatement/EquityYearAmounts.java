package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import java.math.BigDecimal;

public record EquityYearAmounts(
        BigDecimal capital,
        BigDecimal netIncome,
        BigDecimal retainedEarnings,
        BigDecimal reserves,
        BigDecimal totalEquity
) {
}
