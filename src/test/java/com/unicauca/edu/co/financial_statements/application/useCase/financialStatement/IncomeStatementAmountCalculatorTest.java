package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeStatementAmountCalculatorTest {

    private final IncomeStatementAmountCalculator calculator =
            new IncomeStatementAmountCalculator(new AccountingEntryOperations());

    @Test
    void shouldCalculateSignedAmountByIncomeStatementAccountClass() {
        AccountingEntry income = AccountingEntry.builder()
                .accountCode("413505")
                .debit(new BigDecimal("10"))
                .credit(new BigDecimal("100"))
                .build();
        AccountingEntry expense = AccountingEntry.builder()
                .accountCode("510505")
                .debit(new BigDecimal("40"))
                .credit(new BigDecimal("5"))
                .build();
        AccountingEntry balanceSheet = AccountingEntry.builder()
                .accountCode("110505")
                .debit(new BigDecimal("99"))
                .credit(BigDecimal.ZERO)
                .build();

        assertThat(calculator.signedAmount(income)).isEqualByComparingTo("90");
        assertThat(calculator.signedAmount(expense)).isEqualByComparingTo("35");
        assertThat(calculator.signedAmount(balanceSheet)).isEqualByComparingTo("0");
    }

    @Test
    void shouldAggregateByAccountPrefixKeepingAccountIdentity() {
        List<AccountingEntry> entries = List.of(
                AccountingEntry.builder()
                        .accountCode("413505")
                        .accountName("Ventas nacionales")
                        .credit(new BigDecimal("100"))
                        .debit(BigDecimal.ZERO)
                        .build(),
                AccountingEntry.builder()
                        .accountCode("413505")
                        .accountName("Ventas nacionales")
                        .credit(new BigDecimal("60"))
                        .debit(BigDecimal.ZERO)
                        .build(),
                AccountingEntry.builder()
                        .accountCode("421505")
                        .accountName("Recuperaciones")
                        .credit(new BigDecimal("20"))
                        .debit(BigDecimal.ZERO)
                        .build()
        );

        Map<String, BigDecimal> aggregated = calculator.aggregateByAccountPrefix(entries, "4");

        assertThat(aggregated)
                .containsEntry("413505|Ventas nacionales", new BigDecimal("160.00"))
                .containsEntry("421505|Recuperaciones", new BigDecimal("20.00"));
    }

    @Test
    void shouldCalculateNetIncomeOnlyForYearToDate() {
        List<AccountingEntry> entries = List.of(
                AccountingEntry.builder()
                        .date(LocalDate.of(2025, 1, 10))
                        .accountCode("413505")
                        .accountName("Ventas nacionales")
                        .credit(new BigDecimal("200"))
                        .debit(BigDecimal.ZERO)
                        .build(),
                AccountingEntry.builder()
                        .date(LocalDate.of(2025, 2, 15))
                        .accountCode("613505")
                        .accountName("Costo de ventas")
                        .debit(new BigDecimal("80"))
                        .credit(BigDecimal.ZERO)
                        .build(),
                AccountingEntry.builder()
                        .date(LocalDate.of(2025, 3, 20))
                        .accountCode("510505")
                        .accountName("Gastos de administracion")
                        .debit(new BigDecimal("30"))
                        .credit(BigDecimal.ZERO)
                        .build(),
                AccountingEntry.builder()
                        .date(LocalDate.of(2024, 12, 31))
                        .accountCode("413505")
                        .accountName("Ventas ano anterior")
                        .credit(new BigDecimal("999"))
                        .debit(BigDecimal.ZERO)
                        .build()
        );

        BigDecimal netIncome = calculator.calculateNetIncomeForCutoff(entries, LocalDate.of(2025, 3, 31));

        assertThat(netIncome).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldSumEntriesWithMatcherAndAbsoluteFlag() {
        List<AccountingEntry> entries = List.of(
                AccountingEntry.builder()
                        .accountCode("417505")
                        .accountName("Devoluciones")
                        .credit(BigDecimal.ZERO)
                        .debit(new BigDecimal("15"))
                        .build(),
                AccountingEntry.builder()
                        .accountCode("417505")
                        .accountName("Devoluciones")
                        .credit(BigDecimal.ZERO)
                        .debit(new BigDecimal("5"))
                        .build()
        );

        BigDecimal total = calculator.sumEntries(entries, entry -> true, true);

        assertThat(total).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldCalculateYearToDateAmountsFromMockDataset() throws IOException {
        List<AccountingEntry> entries = MockAccountingEntryDatasetLoader.load();

        LocalDate currentCutoffDate = LocalDate.of(2025, 3, 29);
        LocalDate previousCutoffDate = LocalDate.of(2024, 3, 29);

        List<AccountingEntry> currentEntries = entries.stream()
                .filter(entry -> !entry.getDate().isBefore(currentCutoffDate.withDayOfYear(1)))
                .filter(entry -> !entry.getDate().isAfter(currentCutoffDate))
                .toList();
        List<AccountingEntry> previousEntries = entries.stream()
                .filter(entry -> !entry.getDate().isBefore(previousCutoffDate.withDayOfYear(1)))
                .filter(entry -> !entry.getDate().isAfter(previousCutoffDate))
                .toList();

        Map<String, BigDecimal> currentIncomeAccounts = calculator.aggregateByAccountPrefix(currentEntries, "4");
        Map<String, BigDecimal> previousIncomeAccounts = calculator.aggregateByAccountPrefix(previousEntries, "4");

        assertThat(currentIncomeAccounts)
                .containsEntry("413505|Ingresos operacionales - Ventas nacionales", new BigDecimal("175000000.00"))
                .containsEntry("417505|Ingresos operacionales - Devoluciones en ventas", new BigDecimal("-12000000.00"))
                .containsEntry("421505|Otros ingresos - Recuperaciones", new BigDecimal("9000000.00"))
                .containsEntry("421005|Otros ingresos - Intereses", new BigDecimal("5000000.00"));

        assertThat(previousIncomeAccounts)
                .containsEntry("413505|Ingresos operacionales - Ventas nacionales", new BigDecimal("156000000.00"))
                .containsEntry("417505|Ingresos operacionales - Devoluciones en ventas", new BigDecimal("-10000000.00"))
                .containsEntry("421505|Otros ingresos - Recuperaciones", new BigDecimal("7000000.00"))
                .containsEntry("421005|Otros ingresos - Intereses", new BigDecimal("3000000.00"));

        assertThat(calculator.calculateNetIncomeForCutoff(currentEntries, currentCutoffDate))
                .isEqualByComparingTo("12000000.00");
        assertThat(calculator.calculateNetIncomeForCutoff(previousEntries, previousCutoffDate))
                .isEqualByComparingTo("19000000.00");
    }
}
