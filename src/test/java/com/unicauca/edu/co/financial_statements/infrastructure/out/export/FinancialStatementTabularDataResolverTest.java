package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAccount;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementTabularDataResolverTest {

    private final FinancialStatementTabularDataResolver resolver =
            new FinancialStatementTabularDataResolver(new ObjectMapper());

    @Test
    void shouldFlattenTypedRowsAndNestedAccountFields() {
        FinancialStatementRow row = FinancialStatementRow.builder()
                .lineDescription("Caja")
                .currentAmount(new BigDecimal("150000000"))
                .account(FinancialStatementAccount.builder()
                        .accountCode("110505")
                        .accountDescription("Activo corriente - Caja")
                        .nature("DEBITO")
                        .build())
                .build();

        FlattenedFinancialStatementTable table = resolver.resolve(List.of(), List.of(row));

        assertThat(table.rows()).hasSize(1);
        assertThat(table.columns()).contains("lineDescription", "currentAmount", "account.accountCode", "account.accountDescription", "account.nature");
        assertThat(table.rows().get(0))
                .containsEntry("lineDescription", "Caja")
                .containsEntry("account.accountCode", "110505")
                .containsEntry("account.accountDescription", "Activo corriente - Caja")
                .containsEntry("account.nature", "DEBITO");
    }

    @Test
    void shouldPreferTypedRowsOverRawRows() {
        FlattenedFinancialStatementTable table = resolver.resolve(
                List.of(Map.of("legacy", "value")),
                List.of(FinancialStatementRow.builder().lineDescription("Tipada").build())
        );

        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).containsEntry("lineDescription", "Tipada");
        assertThat(table.columns()).contains("lineDescription");
        assertThat(table.columns()).doesNotContain("legacy");
    }
}
