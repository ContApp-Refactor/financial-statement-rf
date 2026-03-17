package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EquityChangesStatementBuilder {

    private final EquityChangesAmountCalculator equityChangesAmountCalculator;
    private final FinancialStatementComparativeRowBuilder financialStatementComparativeRowBuilder;

    public List<Map<String, Object>> buildRows(
            List<AccountingEntry> sourceAccountingEntries,
            List<AccountingEntry> currentAccountingEntries,
            List<AccountingEntry> previousAccountingEntries,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        List<AccountingEntry> sourceEntries = sourceAccountingEntries != null ? sourceAccountingEntries : List.of();
        List<AccountingEntry> currentEntries = currentAccountingEntries != null ? currentAccountingEntries : List.of();
        List<AccountingEntry> previousEntries = previousAccountingEntries != null ? previousAccountingEntries : List.of();
        EquityChangesAmounts amounts = equityChangesAmountCalculator.calculate(
                sourceEntries,
                currentEntries,
                previousEntries,
                currentCutoffDate,
                previousCutoffDate
        );

        BigDecimal currentTotalEquity = amounts.totalPatrimonio().current();
        BigDecimal previousTotalEquity = amounts.totalPatrimonio().previous();

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildEquityComparativeRow(
                "PATRIMONIO",
                null,
                "0",
                "Patrimonio",
                "SECTION",
                null,
                null,
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.totalEquityByYear())
        ));
        rows.add(buildEquityComparativeRow(
                "Capital emitido",
                "31",
                "1",
                "Capital emitido",
                "DETAIL",
                amounts.capitalEmitido().current(),
                amounts.capitalEmitido().previous(),
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.capitalByYear())
        ));
        rows.add(buildEquityComparativeRow(
                "Ganancias del ejercicio",
                "35",
                "2",
                "Ganancias del ejercicio",
                "DETAIL",
                amounts.gananciasEjercicio().current(),
                amounts.gananciasEjercicio().previous(),
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.netIncomeByYear())
        ));
        rows.add(buildEquityComparativeRow(
                "Ganancias acumuladas",
                "36",
                "3",
                "Ganancias acumuladas",
                "DETAIL",
                amounts.gananciasAcumuladas().current(),
                amounts.gananciasAcumuladas().previous(),
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.retainedEarningsByYear())
        ));
        rows.add(buildEquityComparativeRow(
                "Otras reservas",
                "33",
                "4",
                "Otras reservas",
                "DETAIL",
                amounts.otrasReservas().current(),
                amounts.otrasReservas().previous(),
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.reservesByYear())
        ));
        rows.add(buildEquityComparativeRow(
                "Total patrimonio de los accionistas",
                null,
                "9",
                "Total patrimonio de los accionistas",
                "TOTAL",
                currentTotalEquity,
                previousTotalEquity,
                currentTotalEquity,
                previousTotalEquity,
                toYearValueMap(amounts.totalEquityByYear())
        ));

        return rows;
    }

    private Map<String, BigDecimal> toYearValueMap(Map<Integer, BigDecimal> valuesByYear) {
        Map<String, BigDecimal> yearValues = new LinkedHashMap<>();
        if (valuesByYear == null || valuesByYear.isEmpty()) {
            return yearValues;
        }

        valuesByYear.forEach((year, value) -> {
            if (year != null) {
                yearValues.put(String.valueOf(year), scaleAmount(value));
            }
        });
        return yearValues;
    }

    private Map<String, Object> buildEquityComparativeRow(
            String lineDescription,
            String classCode,
            String changeCode,
            String changeDescription,
            String rowType,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal currentTotalEquity,
            BigDecimal previousTotalEquity,
            Map<String, BigDecimal> yearValues
    ) {
        Map<String, Object> row = financialStatementComparativeRowBuilder.buildComparativeRow(
                lineDescription,
                null,
                currentAmount,
                previousAmount,
                rowType,
                currentTotalEquity,
                previousTotalEquity
        );

        boolean section = "SECTION".equalsIgnoreCase(rowType) || "SUBSECTION".equalsIgnoreCase(rowType);
        BigDecimal currentValue = currentAmount != null ? scaleAmount(currentAmount) : null;
        BigDecimal previousValue = previousAmount != null ? scaleAmount(previousAmount) : null;
        BigDecimal variation = (currentValue != null && previousValue != null)
                ? scaleAmount(currentValue.subtract(previousValue))
                : null;

        BigDecimal debitMovement = null;
        BigDecimal creditMovement = null;
        if (!section && variation != null) {
            if (variation.signum() < 0) {
                debitMovement = variation.abs();
                creditMovement = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                debitMovement = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                creditMovement = variation;
            }
        }

        String resolvedClassCode = StringUtils.hasText(classCode) ? classCode : "3";
        String resolvedDescription = StringUtils.hasText(changeDescription) ? changeDescription : lineDescription;

        row.put("changeCode", changeCode);
        row.put("changeDescription", resolvedDescription);
        row.put("classCode", resolvedClassCode);
        row.put("classDescription", resolvedDescription);
        row.put("periodAmount", currentValue);
        row.put("nature", "CREDITO");
        row.put("account", financialStatementComparativeRowBuilder.buildAccount(resolvedClassCode, resolvedDescription, "CREDITO"));
        row.put("initialBalance", previousValue);
        row.put("debitMovement", debitMovement);
        row.put("creditMovement", creditMovement);
        row.put("finalBalance", currentValue);
        if (yearValues != null && !yearValues.isEmpty()) {
            row.put("yearValues", yearValues);
        }
        return row;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
