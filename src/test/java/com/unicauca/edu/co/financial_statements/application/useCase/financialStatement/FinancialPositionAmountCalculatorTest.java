package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPositionAmountCalculatorTest {

    private final AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
    private final FinancialPositionEntryClassifier classifier = new FinancialPositionEntryClassifier(accountingEntryOperations);
    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator =
            new IncomeStatementAmountCalculator(accountingEntryOperations);
    private final PeriodResultCalculator periodResultCalculator =
            new PeriodResultCalculator(accountingEntryOperations, incomeStatementAmountCalculator);
    private final FinancialPositionAmountCalculator calculator =
            new FinancialPositionAmountCalculator(classifier, accountingEntryOperations, periodResultCalculator);

    @Test
    void shouldCalculateFinancialPositionAmountsWithoutMixingTaxAssetAndTaxLiability() {
        List<AccountingEntry> currentEntries = List.of(
                debitEntry(LocalDate.of(2025, 3, 1), "110505", "Caja", "100"),
                debitEntry(LocalDate.of(2025, 3, 2), "135515", "Activos por impuestos corrientes", "20"),
                debitEntry(LocalDate.of(2025, 3, 3), "150405", "Propiedad planta y equipo", "30"),
                creditEntry(LocalDate.of(2025, 3, 4), "210505", "Proveedores", "30"),
                creditEntry(LocalDate.of(2025, 3, 5), "240805", "Pasivos por impuestos corrientes", "15"),
                creditEntry(LocalDate.of(2025, 3, 6), "310505", "Capital social", "60"),
                creditEntry(LocalDate.of(2025, 3, 7), "360505", "Resultados acumulados", "25"),
                debitEntry(LocalDate.of(2025, 3, 8), "370505", "Dividendos decretados", "5"),
                creditEntry(LocalDate.of(2025, 3, 9), "320510", "Prima de emision", "5"),
                creditEntry(LocalDate.of(2025, 1, 10), "413505", "Ventas nacionales", "50"),
                debitEntry(LocalDate.of(2025, 1, 11), "613505", "Costo de ventas", "20"),
                debitEntry(LocalDate.of(2025, 1, 12), "510505", "Gastos de administracion", "10")
        );

        List<AccountingEntry> previousEntries = List.of(
                debitEntry(LocalDate.of(2024, 3, 1), "110505", "Caja", "80"),
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
        );

        FinancialPositionAmounts amounts = calculator.calculate(
                currentEntries,
                previousEntries,
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2024, 3, 31)
        );

        assertThat(amounts.impuestosCorrientes().current()).isEqualByComparingTo("20.00");
        assertThat(amounts.pasivosImpuestosCorrientes().current()).isEqualByComparingTo("15.00");
        assertThat(amounts.utilidadesEjercicio().current()).isEqualByComparingTo("20.00");
        assertThat(amounts.totalActivos().current()).isEqualByComparingTo("150.00");
        assertThat(amounts.totalPasivos().current()).isEqualByComparingTo("45.00");
        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("105.00");

        assertThat(amounts.impuestosCorrientes().previous()).isEqualByComparingTo("10.00");
        assertThat(amounts.pasivosImpuestosCorrientes().previous()).isEqualByComparingTo("12.00");
        assertThat(amounts.utilidadesEjercicio().previous()).isEqualByComparingTo("12.00");
        assertThat(amounts.totalActivos().previous()).isEqualByComparingTo("108.00");
        assertThat(amounts.totalPasivos().previous()).isEqualByComparingTo("30.00");
        assertThat(amounts.totalPatrimonio().previous()).isEqualByComparingTo("78.00");
    }

    @Test
    void shouldCalculateFinancialPositionFromMockDataset() throws IOException {
        List<AccountingEntry> entries = MockAccountingEntryDatasetLoader.load();

        List<AccountingEntry> currentEntries = entries.stream()
                .filter(entry -> !entry.getDate().isAfter(LocalDate.of(2025, 3, 29)))
                .toList();
        List<AccountingEntry> previousEntries = entries.stream()
                .filter(entry -> !entry.getDate().isAfter(LocalDate.of(2024, 3, 29)))
                .toList();

        FinancialPositionAmounts amounts = calculator.calculate(
                currentEntries,
                previousEntries,
                LocalDate.of(2025, 3, 29),
                LocalDate.of(2024, 3, 29)
        );

        assertThat(amounts.totalActivos().current()).isEqualByComparingTo("724000000.00");
        assertThat(amounts.totalPasivos().current()).isEqualByComparingTo("348000000.00");
        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("376000000.00");
        assertThat(amounts.utilidadesEjercicio().current()).isEqualByComparingTo("12000000.00");

        assertThat(amounts.totalActivos().previous()).isEqualByComparingTo("320000000.00");
        assertThat(amounts.totalPasivos().previous()).isEqualByComparingTo("145000000.00");
        assertThat(amounts.totalPatrimonio().previous()).isEqualByComparingTo("175000000.00");
        assertThat(amounts.utilidadesEjercicio().previous()).isEqualByComparingTo("19000000.00");
    }

    @Test
    void shouldUseClosedCurrentPeriodResultWhenThereAreNoOperationalEntriesInTheYear() {
        FinancialPositionAmounts amounts = calculator.calculate(
                List.of(
                        creditEntry(LocalDate.of(2025, 3, 1), "310505", "Capital social", "100"),
                        creditEntry(LocalDate.of(2025, 3, 2), "350505", "Ganancia del ejercicio cerrada", "40")
                ),
                List.of(),
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2024, 3, 31)
        );

        assertThat(amounts.utilidadesEjercicio().current()).isEqualByComparingTo("40.00");
        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("140.00");
    }

    @Test
    void shouldAvoidDoubleCountingClosedCurrentPeriodResultWhenOperationalEntriesExist() {
        FinancialPositionAmounts amounts = calculator.calculate(
                List.of(
                        creditEntry(LocalDate.of(2025, 3, 1), "310505", "Capital social", "100"),
                        creditEntry(LocalDate.of(2025, 3, 2), "350505", "Ganancia del ejercicio cerrada", "40"),
                        creditEntry(LocalDate.of(2025, 1, 10), "413505", "Ventas nacionales", "100"),
                        debitEntry(LocalDate.of(2025, 1, 11), "613505", "Costo de ventas", "40"),
                        debitEntry(LocalDate.of(2025, 1, 12), "510505", "Gastos de administracion", "20")
                ),
                List.of(),
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2024, 3, 31)
        );

        assertThat(amounts.utilidadesEjercicio().current()).isEqualByComparingTo("40.00");
        assertThat(amounts.totalPatrimonio().current()).isEqualByComparingTo("140.00");
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
