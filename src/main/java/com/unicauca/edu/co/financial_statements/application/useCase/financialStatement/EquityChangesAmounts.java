package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EquityChangesAmounts(
        ComparativeAmount capitalEmitido,
        ComparativeAmount gananciasEjercicio,
        ComparativeAmount gananciasAcumuladas,
        ComparativeAmount otrasReservas,
        ComparativeAmount totalPatrimonio,
        Map<Integer, EquityYearAmounts> yearSnapshots
) {

    public EquityChangesAmounts {
        Map<Integer, EquityYearAmounts> safeSnapshots = yearSnapshots != null ? yearSnapshots : Map.of();
        yearSnapshots = Collections.unmodifiableMap(new LinkedHashMap<>(safeSnapshots));
    }

    public Map<Integer, BigDecimal> capitalByYear() {
        return mapYearValues(EquityYearAmounts::capital);
    }

    public Map<Integer, BigDecimal> netIncomeByYear() {
        return mapYearValues(EquityYearAmounts::netIncome);
    }

    public Map<Integer, BigDecimal> retainedEarningsByYear() {
        return mapYearValues(EquityYearAmounts::retainedEarnings);
    }

    public Map<Integer, BigDecimal> reservesByYear() {
        return mapYearValues(EquityYearAmounts::reserves);
    }

    public Map<Integer, BigDecimal> totalEquityByYear() {
        return mapYearValues(EquityYearAmounts::totalEquity);
    }

    private Map<Integer, BigDecimal> mapYearValues(java.util.function.Function<EquityYearAmounts, BigDecimal> extractor) {
        Map<Integer, BigDecimal> values = new LinkedHashMap<>();
        yearSnapshots.forEach((year, snapshot) -> values.put(year, extractor.apply(snapshot)));
        return values;
    }
}
