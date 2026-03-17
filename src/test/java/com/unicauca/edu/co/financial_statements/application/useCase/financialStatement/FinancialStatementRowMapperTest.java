package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementRowMapperTest {

    private final FinancialStatementRowMapper mapper = new FinancialStatementRowMapper();

    @Test
    void shouldConvertLegacyMapRowToTypedRowPreservingEquitySpecificFields() {
        FinancialStatementRow row = mapper.toTypedRow(Map.of(
                "lineDescription", "Ganancias acumuladas",
                "rowType", "DETAIL",
                "currentAmount", new BigDecimal("40.00"),
                "yearValues", Map.of(
                        "2024", new BigDecimal("20.00"),
                        "2025", new BigDecimal("40.00")
                )
        ));

        assertThat(row).isNotNull();
        assertThat(row.getLineDescription()).isEqualTo("Ganancias acumuladas");
        assertThat(row.getCurrentAmount()).isEqualByComparingTo("40.00");
        assertThat(row.getYearValues())
                .containsEntry("2024", new BigDecimal("20.00"))
                .containsEntry("2025", new BigDecimal("40.00"));
    }

    @Test
    void shouldConvertTypedRowsBackToLegacyMapsForCompatibility() {
        FinancialStatementRow row = FinancialStatementRow.builder()
                .lineDescription("ACTIVO")
                .rowType("SECTION")
                .currentAmount(new BigDecimal("100.00"))
                .build();

        List<Map<String, Object>> rows = mapper.toRowMaps(List.of(row));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0))
                .containsEntry("lineDescription", "ACTIVO")
                .containsEntry("rowType", "SECTION")
                .containsEntry("currentAmount", new BigDecimal("100.00"));
    }
}
