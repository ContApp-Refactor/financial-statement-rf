package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import java.math.BigDecimal;
import java.util.Map;

record EquityMatrixRow(
        String description,
        String rowType,
        Map<Integer, BigDecimal> valuesByYear
) {
}
