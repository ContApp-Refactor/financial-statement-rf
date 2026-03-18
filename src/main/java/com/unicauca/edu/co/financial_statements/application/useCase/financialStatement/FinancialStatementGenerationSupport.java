package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteriaRange;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementCriteriaType;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FinancialStatementGenerationSupport {

    private final AccountingEntryOperations accountingEntryOperations;

    public List<AccountingEntry> filterEntriesUpToCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return List.of();
        }

        return entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null && !entry.getDate().isAfter(cutoffDate))
                .toList();
    }

    public List<AccountingEntry> applyCriteriaLevelFilter(
            List<AccountingEntry> entries,
            FinancialStatementCriteria criteria,
            boolean preserveIncomeStatementAccounts
    ) {
        if (entries == null || entries.isEmpty() || criteria == null) {
            return entries != null ? entries : List.of();
        }

        FinancialStatementCriteriaRange criteriaRange = criteria.getCriteriaRange();
        if (!StringUtils.hasText(criteria.getCriteriaType())
                || criteriaRange == null
                || criteriaRange.getFrom() == null
                || criteriaRange.getTo() == null) {
            return entries;
        }

        int prefixLength = EFinancialStatementCriteriaType.resolvePrefixLength(criteria.getCriteriaType());
        if (prefixLength <= 0) {
            return entries;
        }

        long from = criteriaRange.getFrom();
        long to = criteriaRange.getTo();

        return entries.stream()
                .filter(Objects::nonNull)
                .filter(entry -> accountingEntryOperations.matchesCriteriaRange(entry, prefixLength, from, to)
                        || (preserveIncomeStatementAccounts && accountingEntryOperations.isIncomeStatementAccount(entry)))
                .toList();
    }

    public void validateCutoffDatesAgainstLatestMovement(
            List<AccountingEntry> entries,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        LocalDate latestMovementDate = entries.stream()
                .map(AccountingEntry::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (latestMovementDate != null
                && previousCutoffDate != null
                && currentCutoffDate != null
                && previousCutoffDate.isAfter(latestMovementDate)
                && currentCutoffDate.isAfter(latestMovementDate)) {
            throw new IllegalArgumentException(
                    "Selected cutoff dates are after the latest accounting movement date (" + latestMovementDate
                            + "). Choose cutoff dates up to " + latestMovementDate + "."
            );
        }
    }

    public void validateBalancedFinancialPosition(
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal totalEquity,
            LocalDate cutoffDate,
            String periodLabel
    ) {
        BigDecimal assets = scaleAmount(totalAssets);
        BigDecimal liabilities = scaleAmount(totalLiabilities);
        BigDecimal equity = scaleAmount(totalEquity);
        BigDecimal difference = scaleAmount(assets.subtract(liabilities.add(equity)));

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            String resolvedCutoff = cutoffDate != null ? cutoffDate.toString() : "without cutoff date";
            throw new IllegalArgumentException(
                    "The " + periodLabel + " financial position does not balance at cutoff " + resolvedCutoff
                            + ". Assets=" + assets
                            + ", liabilities=" + liabilities
                            + ", equity=" + equity
                            + ", difference=" + difference
                            + ". Review the accounting source before generating the report."
            );
        }
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
