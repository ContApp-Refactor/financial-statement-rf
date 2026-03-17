package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementPositionTableBuilderTest {

    private final FinancialStatementPositionTableBuilder builder = new FinancialStatementPositionTableBuilder();

    @Test
    void shouldDetectFinancialPositionRowsAndMapThem() {
        FinancialStatementRow row = new FinancialStatementRow();
        row.setLineDescription("TOTAL ACTIVO");
        row.setRowType("TOTAL");
        row.setCurrentAmount(new BigDecimal("100"));
        row.setPreviousAmount(new BigDecimal("90"));

        assertThat(builder.supports(List.of(row))).isTrue();

        List<FinancialPositionRow> mappedRows = builder.toRows(List.of(row));
        assertThat(mappedRows).hasSize(1);
        assertThat(mappedRows.get(0).lineDescription()).isEqualTo("TOTAL ACTIVO");
        assertThat(mappedRows.get(0).currentAmount()).isEqualByComparingTo("100");
    }

    @Test
    void shouldResolveTotalFormulasForKnownTotalsAndSubsections() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        totals.put("TOTAL ACTIVO CORRIENTE", 10);
        totals.put("TOTAL ACTIVO NO CORRIENTE", 20);
        totals.put("TOTAL PASIVO", 30);
        totals.put("TOTAL ACTIVO", 40);
        totals.put("UTILIDAD BRUTA", 50);
        totals.put("TOTAL GASTOS OPERACIONALES", 60);

        assertThat(builder.resolveTotalAmountFormula("TOTAL ACTIVO", 'B', 40, 5, totals))
                .isEqualTo("B10+B20");
        assertThat(builder.resolveTotalAmountFormula("TOTAL PATRIMONIO", 'D', 41, 5, totals))
                .isEqualTo("D40-D30");
        assertThat(builder.resolveTotalAmountFormula("UTILIDAD OPERACIONAL", 'B', 61, 5, totals))
                .isEqualTo("B50-B60");
        assertThat(builder.resolveTotalAmountFormula("TOTAL PASIVO CORRIENTE", 'B', 70, 65, totals))
                .isEqualTo("SUM(B66:B69)");
    }

    @Test
    void shouldExposeSectionAndTotalRowHelpers() {
        assertThat(builder.isSectionRow("SECTION")).isTrue();
        assertThat(builder.isSectionRow("SUBSECTION")).isTrue();
        assertThat(builder.isTotalRow("TOTAL")).isTrue();
        assertThat(builder.normalizeLabel(" total   activo ")).isEqualTo("TOTAL ACTIVO");
    }
}
