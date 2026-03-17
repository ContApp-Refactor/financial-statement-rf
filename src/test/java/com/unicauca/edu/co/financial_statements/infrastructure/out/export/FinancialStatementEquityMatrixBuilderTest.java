package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementEquityMatrixBuilderTest {

    private final FinancialStatementExportCriteriaResolver criteriaResolver =
            new FinancialStatementExportCriteriaResolver(new ObjectMapper());
    private final FinancialStatementEquityMatrixBuilder builder =
            new FinancialStatementEquityMatrixBuilder(criteriaResolver);

    @Test
    void shouldResolveYearsFromCriteriaAndTypedRows() {
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("criteria", Map.of(
                "startDate", "2024-03-29",
                "endDate", "2025-03-29"
        ));

        FinancialStatementRow row = new FinancialStatementRow();
        row.setYearValues(Map.of("2026", new BigDecimal("15")));

        List<Integer> years = builder.resolveYears(List.of(row), financialStatement);

        assertThat(years).containsExactly(2024, 2025, 2026);
    }

    @Test
    void shouldNormalizeAndOrderEquityRows() {
        FinancialStatementRow reserves = new FinancialStatementRow();
        reserves.setLineDescription("Reserva legal");
        reserves.setRowType("DETAIL");
        reserves.setYearValues(Map.of("2025", new BigDecimal("10")));

        FinancialStatementRow capital = new FinancialStatementRow();
        capital.setLineDescription("Capital social");
        capital.setRowType("DETAIL");
        capital.setYearValues(Map.of("2025", new BigDecimal("50")));

        FinancialStatementRow total = new FinancialStatementRow();
        total.setLineDescription("TOTAL PATRIMONIO");
        total.setRowType("TOTAL");
        total.setYearValues(Map.of("2025", new BigDecimal("60")));

        List<EquityMatrixRow> matrixRows = builder.buildMatrixRows(
                List.of(reserves, total, capital),
                List.of(2025)
        );

        assertThat(matrixRows)
                .extracting(EquityMatrixRow::description)
                .containsExactly("Capital emitido", "Otras reservas", "Total patrimonio de los accionistas");

        assertThat(matrixRows.get(0).valuesByYear()).containsEntry(2025, new BigDecimal("50.00"));
        assertThat(matrixRows.get(2).valuesByYear()).containsEntry(2025, new BigDecimal("60.00"));
    }

    @Test
    void shouldFallbackToCurrentAndPreviousAmountsWhenYearValuesAreMissing() {
        FinancialStatementRow row = new FinancialStatementRow();
        row.setLineDescription("Ganancias acumuladas");
        row.setRowType("DETAIL");
        row.setPreviousAmount(new BigDecimal("40"));
        row.setCurrentAmount(new BigDecimal("55"));

        List<EquityMatrixRow> matrixRows = builder.buildMatrixRows(List.of(row), List.of(2024, 2025));

        assertThat(matrixRows).hasSize(1);
        assertThat(matrixRows.get(0).valuesByYear())
                .containsEntry(2024, new BigDecimal("40.00"))
                .containsEntry(2025, new BigDecimal("55.00"));
    }
}
