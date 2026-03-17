package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPositionStatementBuilderTest {

    private final AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
    private final FinancialStatementComparativeRowBuilder comparativeRowBuilder =
            new FinancialStatementComparativeRowBuilder(accountingEntryOperations);
    private final FinancialPositionEntryClassifier classifier = new FinancialPositionEntryClassifier(accountingEntryOperations);
    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator =
            new IncomeStatementAmountCalculator(accountingEntryOperations);
    private final FinancialPositionAmountCalculator amountCalculator =
            new FinancialPositionAmountCalculator(classifier, accountingEntryOperations, incomeStatementAmountCalculator);
    private final FinancialPositionStatementBuilder builder =
            new FinancialPositionStatementBuilder(
                    amountCalculator,
                    classifier,
                    comparativeRowBuilder,
                    accountingEntryOperations
            );

    @Test
    void shouldBuildFinancialPositionRowsAndTotals() {
        FinancialStatementCriteria criteria = FinancialStatementCriteria.builder()
                .criteriaType("ACCOUNT")
                .build();

        FinancialPositionRowsResult result = builder.build(
                List.of(
                        debitEntry(LocalDate.of(2025, 3, 1), "110505", "Activo corriente - Caja principal", "70"),
                        debitEntry(LocalDate.of(2025, 3, 2), "110510", "Activo corriente - Caja menor", "30"),
                        debitEntry(LocalDate.of(2025, 3, 3), "135515", "Activos por impuestos corrientes", "20"),
                        debitEntry(LocalDate.of(2025, 3, 4), "150405", "Propiedad planta y equipo", "30"),
                        creditEntry(LocalDate.of(2025, 3, 5), "210505", "Proveedores", "30"),
                        creditEntry(LocalDate.of(2025, 3, 6), "240805", "Pasivos por impuestos corrientes", "15"),
                        creditEntry(LocalDate.of(2025, 3, 7), "310505", "Capital social", "60"),
                        creditEntry(LocalDate.of(2025, 3, 8), "360505", "Resultados acumulados", "25"),
                        debitEntry(LocalDate.of(2025, 3, 9), "370505", "Dividendos decretados", "5"),
                        creditEntry(LocalDate.of(2025, 3, 10), "320510", "Prima de emision", "5"),
                        creditEntry(LocalDate.of(2025, 1, 10), "413505", "Ventas nacionales", "50"),
                        debitEntry(LocalDate.of(2025, 1, 11), "613505", "Costo de ventas", "20"),
                        debitEntry(LocalDate.of(2025, 1, 12), "510505", "Gastos de administracion", "10")
                ),
                List.of(
                        debitEntry(LocalDate.of(2024, 3, 1), "110505", "Activo corriente - Caja principal", "80"),
                        debitEntry(LocalDate.of(2024, 3, 2), "135515", "Activos por impuestos corrientes", "10"),
                        debitEntry(LocalDate.of(2024, 3, 3), "150405", "Propiedad planta y equipo", "18"),
                        creditEntry(LocalDate.of(2024, 3, 4), "210505", "Proveedores", "18"),
                        creditEntry(LocalDate.of(2024, 3, 5), "240805", "Pasivos por impuestos corrientes", "12"),
                        creditEntry(LocalDate.of(2024, 3, 6), "310505", "Capital social", "50"),
                        creditEntry(LocalDate.of(2024, 3, 7), "360505", "Resultados acumulados", "15"),
                        debitEntry(LocalDate.of(2024, 3, 8), "370505", "Dividendos decretados", "3"),
                        creditEntry(LocalDate.of(2024, 3, 9), "320510", "Prima de emision", "4"),
                        creditEntry(LocalDate.of(2024, 1, 10), "413505", "Ventas nacionales", "30"),
                        debitEntry(LocalDate.of(2024, 1, 11), "613505", "Costo de ventas", "12"),
                        debitEntry(LocalDate.of(2024, 1, 12), "510505", "Gastos de administracion", "6")
                ),
                criteria,
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2024, 3, 31)
        );

        assertThat(result.totalAssets()).isEqualByComparingTo("150.00");
        assertThat(result.totalLiabilities()).isEqualByComparingTo("45.00");
        assertThat(result.totalEquity()).isEqualByComparingTo("105.00");
        assertThat(result.previousTotalAssets()).isEqualByComparingTo("108.00");
        assertThat(result.previousTotalLiabilities()).isEqualByComparingTo("30.00");
        assertThat(result.previousTotalEquity()).isEqualByComparingTo("78.00");

        assertThat(result.rows().get(0)).containsEntry("lineDescription", "ACTIVO");
        assertThat(result.rows()).anySatisfy(row -> assertThat(row)
                .containsEntry("lineDescription", "TOTAL PASIVO + PATRIMONIO")
                .containsEntry("currentAmount", new BigDecimal("150.00"))
                .containsEntry("previousAmount", new BigDecimal("108.00")));
        assertThat(result.rows()).anyMatch(this::isGroupedCashAccountRow);
    }

    private boolean isGroupedCashAccountRow(Map<String, Object> row) {
        return "  1105 - Activo corriente".equals(row.get("lineDescription"))
                && new BigDecimal("100.00").compareTo((BigDecimal) row.get("currentAmount")) == 0;
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

    private AccountingEntry creditEntry(LocalDate date, String code, String name, String amount) {
        return AccountingEntry.builder()
                .date(date)
                .accountCode(code)
                .accountName(name)
                .accountNature("CREDITO")
                .debit(BigDecimal.ZERO)
                .credit(new BigDecimal(amount))
                .build();
    }
}
