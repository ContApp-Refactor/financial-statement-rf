package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EquityChangesAmountCalculatorTest {

    private final EquityChangesAmountCalculator calculator = new EquityChangesAmountCalculator(
            new AccountingEntryOperations(),
            new IncomeStatementAmountCalculator(new AccountingEntryOperations())
    );

    @Test
    void shouldCalculateComparativeEquityAmountsAndYearSnapshots() {
        List<AccountingEntry> sourceEntries = List.of(
                creditEntry("2024-01-10", "310505", "Capital social", "80"),
                creditEntry("2024-01-15", "320510", "Prima de emision", "10"),
                creditEntry("2024-01-20", "330505", "Reserva legal", "8"),
                creditEntry("2024-01-25", "360505", "Resultados acumulados", "20"),
                debitEntry("2024-02-05", "370505", "Dividendos decretados", "4"),
                debitEntry("2024-02-10", "320505", "Acciones propias readquiridas", "1"),
                creditEntry("2024-03-01", "413505", "Ventas nacionales", "40"),
                debitEntry("2024-03-10", "613505", "Costo de ventas", "15"),
                debitEntry("2024-03-15", "510505", "Gastos de administracion", "5"),
                creditEntry("2025-01-10", "310505", "Capital social", "100"),
                creditEntry("2025-01-15", "320510", "Prima de emision", "20"),
                creditEntry("2025-01-20", "330505", "Reserva legal", "12"),
                creditEntry("2025-01-25", "360505", "Resultados acumulados", "40"),
                debitEntry("2025-02-05", "370505", "Dividendos decretados", "6"),
                debitEntry("2025-02-10", "320505", "Acciones propias readquiridas", "3"),
                creditEntry("2025-03-01", "413505", "Ventas nacionales", "60"),
                debitEntry("2025-03-10", "613505", "Costo de ventas", "25"),
                debitEntry("2025-03-15", "510505", "Gastos de administracion", "10")
        );

        List<AccountingEntry> previousEntries = sourceEntries.stream()
                .filter(entry -> entry.getDate() != null && !entry.getDate().isAfter(LocalDate.of(2024, 3, 31)))
                .toList();
        List<AccountingEntry> currentEntries = sourceEntries.stream()
                .filter(entry -> entry.getDate() != null && !entry.getDate().isAfter(LocalDate.of(2025, 3, 31)))
                .toList();

        EquityChangesAmounts amounts = calculator.calculate(
                sourceEntries,
                currentEntries,
                previousEntries,
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2024, 3, 31)
        );

        assertThat(amounts.capitalEmitido().current()).isEqualByComparingTo("210.00");
        assertThat(amounts.capitalEmitido().previous()).isEqualByComparingTo("90.00");

        assertThat(amounts.otrasReservas().current()).isEqualByComparingTo("20.00");
        assertThat(amounts.otrasReservas().previous()).isEqualByComparingTo("8.00");

        assertThat(amounts.gananciasAcumuladas().current()).isEqualByComparingTo("46.00");
        assertThat(amounts.gananciasAcumuladas().previous()).isEqualByComparingTo("15.00");

        assertThat(amounts.gananciasEjercicio().current()).isEqualByComparingTo("25.00");
        assertThat(amounts.gananciasEjercicio().previous()).isEqualByComparingTo("20.00");

        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("301.00");
        assertThat(amounts.totalPatrimonio().previous()).isEqualByComparingTo("133.00");

        assertThat(amounts.capitalByYear())
                .containsEntry(2024, new BigDecimal("90.00"))
                .containsEntry(2025, new BigDecimal("210.00"));
        assertThat(amounts.netIncomeByYear())
                .containsEntry(2024, new BigDecimal("20.00"))
                .containsEntry(2025, new BigDecimal("25.00"));
        assertThat(amounts.totalEquityByYear())
                .containsEntry(2024, new BigDecimal("133.00"))
                .containsEntry(2025, new BigDecimal("301.00"));
    }

    @Test
    void shouldCalculateEquityChangesFromMockDataset() throws IOException {
        List<AccountingEntry> sourceEntries = MockAccountingEntryDatasetLoader.load();
        List<AccountingEntry> previousEntries = sourceEntries.stream()
                .filter(entry -> entry.getDate() != null && !entry.getDate().isAfter(LocalDate.of(2024, 3, 29)))
                .toList();
        List<AccountingEntry> currentEntries = sourceEntries.stream()
                .filter(entry -> entry.getDate() != null && !entry.getDate().isAfter(LocalDate.of(2025, 3, 29)))
                .toList();

        EquityChangesAmounts amounts = calculator.calculate(
                sourceEntries,
                currentEntries,
                previousEntries,
                LocalDate.of(2025, 3, 29),
                LocalDate.of(2024, 3, 29)
        );

        assertThat(amounts.capitalEmitido().current()).isEqualByComparingTo("229000000.00");
        assertThat(amounts.capitalEmitido().previous()).isEqualByComparingTo("107000000.00");

        assertThat(amounts.otrasReservas().current()).isEqualByComparingTo("21000000.00");
        assertThat(amounts.otrasReservas().previous()).isEqualByComparingTo("9000000.00");

        assertThat(amounts.gananciasAcumuladas().current()).isEqualByComparingTo("114000000.00");
        assertThat(amounts.gananciasAcumuladas().previous()).isEqualByComparingTo("40000000.00");

        assertThat(amounts.gananciasEjercicio().current()).isEqualByComparingTo("12000000.00");
        assertThat(amounts.gananciasEjercicio().previous()).isEqualByComparingTo("19000000.00");

        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("376000000.00");
        assertThat(amounts.totalPatrimonio().previous()).isEqualByComparingTo("175000000.00");

        assertThat(amounts.capitalByYear())
                .containsEntry(2024, new BigDecimal("107000000.00"))
                .containsEntry(2025, new BigDecimal("229000000.00"));
        assertThat(amounts.netIncomeByYear())
                .containsEntry(2024, new BigDecimal("19000000.00"))
                .containsEntry(2025, new BigDecimal("12000000.00"));
        assertThat(amounts.totalEquityByYear())
                .containsEntry(2024, new BigDecimal("175000000.00"))
                .containsEntry(2025, new BigDecimal("376000000.00"));
    }

    private AccountingEntry debitEntry(String date, String code, String name, String debit) {
        return AccountingEntry.builder()
                .date(LocalDate.parse(date))
                .accountCode(code)
                .accountName(name)
                .accountNature("DEBITO")
                .debit(new BigDecimal(debit))
                .credit(BigDecimal.ZERO)
                .build();
    }

    private AccountingEntry creditEntry(String date, String code, String name, String credit) {
        return AccountingEntry.builder()
                .date(LocalDate.parse(date))
                .accountCode(code)
                .accountName(name)
                .accountNature("CREDITO")
                .debit(BigDecimal.ZERO)
                .credit(new BigDecimal(credit))
                .build();
    }
}
