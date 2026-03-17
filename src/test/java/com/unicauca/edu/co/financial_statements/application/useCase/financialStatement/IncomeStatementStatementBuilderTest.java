package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeStatementStatementBuilderTest {

    private final AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
    private final IncomeStatementAmountCalculator amountCalculator =
            new IncomeStatementAmountCalculator(accountingEntryOperations);
    private final IncomeStatementEntryClassifier classifier =
            new IncomeStatementEntryClassifier(accountingEntryOperations);
    private final FinancialStatementComparativeRowBuilder comparativeRowBuilder =
            new FinancialStatementComparativeRowBuilder(accountingEntryOperations);
    private final IncomeStatementStatementBuilder builder =
            new IncomeStatementStatementBuilder(amountCalculator, classifier, comparativeRowBuilder);

    @Test
    void shouldBuildIncomeStatementRowsWithOrdinaryIncomeAsPercentageBase() {
        FinancialStatementCriteria criteria = FinancialStatementCriteria.builder()
                .criteriaType("ACCOUNT")
                .build();

        List<Map<String, Object>> rows = builder.buildRows(
                List.of(
                        creditEntry(LocalDate.of(2025, 1, 10), "413505", "Ingresos operacionales - Ventas nacionales", "175"),
                        debitEntry(LocalDate.of(2025, 2, 10), "417505", "Ingresos operacionales - Devoluciones en ventas", "12"),
                        debitEntry(LocalDate.of(2025, 2, 11), "613505", "Costo de ventas", "95"),
                        creditEntry(LocalDate.of(2025, 2, 12), "421505", "Otros ingresos - Recuperaciones", "9"),
                        debitEntry(LocalDate.of(2025, 2, 13), "510505", "Gastos de administracion", "30"),
                        debitEntry(LocalDate.of(2025, 2, 14), "520505", "Gastos de ventas", "15"),
                        debitEntry(LocalDate.of(2025, 2, 15), "530505", "Gastos financieros - Intereses", "6"),
                        debitEntry(LocalDate.of(2025, 2, 16), "516005", "Gastos de administracion - Depreciacion", "4"),
                        debitEntry(LocalDate.of(2025, 2, 17), "540505", "Impuesto de renta", "12")
                ),
                List.of(
                        creditEntry(LocalDate.of(2024, 1, 10), "413505", "Ingresos operacionales - Ventas nacionales", "156"),
                        debitEntry(LocalDate.of(2024, 2, 10), "417505", "Ingresos operacionales - Devoluciones en ventas", "10"),
                        debitEntry(LocalDate.of(2024, 2, 11), "613505", "Costo de ventas", "82"),
                        creditEntry(LocalDate.of(2024, 2, 12), "421505", "Otros ingresos - Recuperaciones", "7"),
                        debitEntry(LocalDate.of(2024, 2, 13), "510505", "Gastos de administracion", "25"),
                        debitEntry(LocalDate.of(2024, 2, 14), "520505", "Gastos de ventas", "12"),
                        debitEntry(LocalDate.of(2024, 2, 15), "530505", "Gastos financieros - Intereses", "4.5"),
                        debitEntry(LocalDate.of(2024, 2, 16), "516005", "Gastos de administracion - Depreciacion", "3.5"),
                        debitEntry(LocalDate.of(2024, 2, 17), "540505", "Impuesto de renta", "8")
                ),
                criteria
        );

        assertThat(row("Ingresos ordinarios", rows))
                .containsEntry("currentAmount", new BigDecimal("175.00"))
                .containsEntry("currentPercentage", new BigDecimal("100.00"));
        assertThat(row("(-) Devoluciones en ventas", rows))
                .containsEntry("currentAmount", new BigDecimal("12.00"))
                .containsEntry("currentPercentage", new BigDecimal("6.86"));
        assertThat(row("INGRESOS NETOS OPERACIONALES", rows))
                .containsEntry("currentAmount", new BigDecimal("163.00"))
                .containsEntry("currentPercentage", new BigDecimal("93.14"));
        assertThat(row("RESULTADO DEL EJERCICIO", rows))
                .containsEntry("currentAmount", new BigDecimal("10.00"));
        assertThat(rows).anySatisfy(row -> assertThat(row)
                .containsEntry("lineDescription", "  4135 - Ingresos operacionales - Ventas nacionales")
                .containsEntry("currentAmount", new BigDecimal("175.00")));
    }

    private Map<String, Object> row(String lineDescription, List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(row -> lineDescription.equals(row.get("lineDescription")))
                .findFirst()
                .orElseThrow();
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
