package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementExportRowMapperTest {

    private final FinancialStatementExportRowMapper mapper = new FinancialStatementExportRowMapper();

    @Test
    void shouldMapRawRowsToTypedRows() {
        List<FinancialStatementRow> rows = mapper.toRows(List.of(Map.of(
                "lineDescription", "Capital emitido",
                "rowType", "DETAIL",
                "currentAmount", new BigDecimal("120000000.00"),
                "yearValues", Map.of("2024", new BigDecimal("100000000.00")),
                "account", Map.of(
                        "accountCode", "310505",
                        "accountDescription", "Capital social",
                        "nature", "CREDITO"
                )
        )));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getLineDescription()).isEqualTo("Capital emitido");
        assertThat(rows.get(0).getCurrentAmount()).isEqualByComparingTo("120000000.00");
        assertThat(rows.get(0).getYearValues()).containsEntry("2024", new BigDecimal("100000000.00"));
        assertThat(rows.get(0).getAccount()).isNotNull();
        assertThat(rows.get(0).getAccount().getAccountCode()).isEqualTo("310505");
    }
}
