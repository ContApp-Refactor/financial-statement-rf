package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import java.math.BigDecimal;

public record ComparativeAmount(
        BigDecimal current,
        BigDecimal previous
) {
}
