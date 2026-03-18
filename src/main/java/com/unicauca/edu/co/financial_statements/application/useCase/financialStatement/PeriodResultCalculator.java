package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class PeriodResultCalculator {

    private final AccountingEntryOperations accountingEntryOperations;
    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator;

    public PeriodResultCalculator(
            AccountingEntryOperations accountingEntryOperations,
            IncomeStatementAmountCalculator incomeStatementAmountCalculator
    ) {
        this.accountingEntryOperations = accountingEntryOperations;
        this.incomeStatementAmountCalculator = incomeStatementAmountCalculator;
    }

    public BigDecimal resolveResultForCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return scaleAmount(BigDecimal.ZERO);
        }

        List<AccountingEntry> entriesUpToCutoff = entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null && !entry.getDate().isAfter(cutoffDate))
                .toList();

        if (entriesUpToCutoff.isEmpty()) {
            return scaleAmount(BigDecimal.ZERO);
        }

        if (hasIncomeStatementMovementsInCurrentYear(entriesUpToCutoff, cutoffDate)) {
            return incomeStatementAmountCalculator.calculateNetIncomeForCutoff(entriesUpToCutoff, cutoffDate);
        }

        BigDecimal closedPeriodResult = entriesUpToCutoff.stream()
                .filter(entry -> accountingEntryOperations.codeStartsWith(entry, "35"))
                .map(accountingEntryOperations::signedAmountByNature)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return scaleAmount(closedPeriodResult);
    }

    private boolean hasIncomeStatementMovementsInCurrentYear(List<AccountingEntry> entries, LocalDate cutoffDate) {
        LocalDate periodStart = cutoffDate.withDayOfYear(1);

        return entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null)
                .filter(entry -> !entry.getDate().isBefore(periodStart) && !entry.getDate().isAfter(cutoffDate))
                .anyMatch(accountingEntryOperations::isIncomeStatementAccount);
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
