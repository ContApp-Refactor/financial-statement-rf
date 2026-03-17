package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementComparativeRowBuilderTest {

    private final AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
    private final FinancialStatementComparativeRowBuilder builder =
            new FinancialStatementComparativeRowBuilder(accountingEntryOperations);

    @Test
    void shouldBuildComparativeRowWithPercentagesAndVariation() {
        Map<String, Object> row = builder.buildComparativeRow(
                "TOTAL ACTIVOS",
                null,
                new BigDecimal("150.00"),
                new BigDecimal("100.00"),
                "TOTAL",
                new BigDecimal("300.00"),
                new BigDecimal("200.00")
        );

        assertThat(row)
                .containsEntry("lineDescription", "TOTAL ACTIVOS")
                .containsEntry("currentPercentage", new BigDecimal("50.00"))
                .containsEntry("previousPercentage", new BigDecimal("50.00"))
                .containsEntry("variation", new BigDecimal("50.00"))
                .containsEntry("variationPercentage", new BigDecimal("0.00"));
    }

    @Test
    void shouldBuildLevelRowsGroupedByCriteriaLevel() {
        FinancialStatementCriteria criteria = FinancialStatementCriteria.builder()
                .criteriaType("ACCOUNT")
                .build();

        List<Map<String, Object>> rows = builder.buildLevelComparisonRows(
                List.of(
                        debitEntry(LocalDate.of(2025, 3, 1), "110505", "Activo corriente - Caja principal", "100"),
                        debitEntry(LocalDate.of(2025, 3, 2), "110510", "Activo corriente - Caja menor", "50")
                ),
                List.of(
                        debitEntry(LocalDate.of(2024, 3, 1), "110505", "Activo corriente - Caja principal", "80")
                ),
                criteria,
                entry -> true,
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                accountingEntryOperations::signedAmountByNature,
                true
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0))
                .containsEntry("lineDescription", "  1105 - Activo corriente")
                .containsEntry("currentAmount", new BigDecimal("150.00"))
                .containsEntry("previousAmount", new BigDecimal("80.00"));
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) rows.get(0).get("account");

        assertThat(account)
                .containsEntry("accountCode", "1105")
                .containsEntry("accountDescription", "Activo corriente");
    }

    private AccountingEntry debitEntry(LocalDate date, String code, String name, String amount) {
        return AccountingEntry.builder()
                .date(date)
                .accountCode(code)
                .accountName(name)
                .accountNature("DEBITO")
                .debit(new BigDecimal(amount))
                .credit(BigDecimal.ZERO)
                .build();
    }
}
