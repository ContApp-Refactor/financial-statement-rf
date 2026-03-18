package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodResultCalculatorTest {

    private final AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator =
            new IncomeStatementAmountCalculator(accountingEntryOperations);
    private final PeriodResultCalculator calculator =
            new PeriodResultCalculator(accountingEntryOperations, incomeStatementAmountCalculator);

    @Test
    void shouldUseClosedResultAccountWhenOperationalEntriesDoNotExist() {
        BigDecimal result = calculator.resolveResultForCutoff(
                List.of(
                        creditEntry(LocalDate.of(2025, 3, 20), "350505", "Ganancia del ejercicio cerrada", "55")
                ),
                LocalDate.of(2025, 3, 31)
        );

        assertThat(result).isEqualByComparingTo("55.00");
    }

    @Test
    void shouldPreferOperationalYearToDateResultToAvoidDoubleCountingClosedBalance() {
        BigDecimal result = calculator.resolveResultForCutoff(
                List.of(
                        creditEntry(LocalDate.of(2025, 3, 20), "350505", "Ganancia del ejercicio cerrada", "40"),
                        creditEntry(LocalDate.of(2025, 1, 10), "413505", "Ventas nacionales", "100"),
                        debitEntry(LocalDate.of(2025, 1, 11), "613505", "Costo de ventas", "40"),
                        debitEntry(LocalDate.of(2025, 1, 12), "510505", "Gastos de administracion", "20")
                ),
                LocalDate.of(2025, 3, 31)
        );

        assertThat(result).isEqualByComparingTo("40.00");
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
