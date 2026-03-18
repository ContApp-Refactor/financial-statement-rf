package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class EquityChangesAmountCalculator {

    private final AccountingEntryOperations accountingEntryOperations;
    private final FinancialPositionEntryClassifier financialPositionEntryClassifier;
    private final PeriodResultCalculator periodResultCalculator;

    public EquityChangesAmountCalculator(
            AccountingEntryOperations accountingEntryOperations,
            FinancialPositionEntryClassifier financialPositionEntryClassifier,
            PeriodResultCalculator periodResultCalculator
    ) {
        this.accountingEntryOperations = accountingEntryOperations;
        this.financialPositionEntryClassifier = financialPositionEntryClassifier;
        this.periodResultCalculator = periodResultCalculator;
    }

    public EquityChangesAmounts calculate(
            List<AccountingEntry> sourceEntries,
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        List<AccountingEntry> safeSourceEntries = sourceEntries != null ? sourceEntries : List.of();
        List<AccountingEntry> safeCurrentEntries = currentEntries != null ? currentEntries : List.of();
        List<AccountingEntry> safePreviousEntries = previousEntries != null ? previousEntries : List.of();

        BigDecimal currentCapitalBase = sumEquityComponentValue(safeCurrentEntries, "31");
        BigDecimal previousCapitalBase = sumEquityComponentValue(safePreviousEntries, "31");

        BigDecimal currentReserves = sumEquityComponentValue(safeCurrentEntries, "33");
        BigDecimal previousReserves = sumEquityComponentValue(safePreviousEntries, "33");

        BigDecimal currentRetainedBase = sumEquityComponentValue(safeCurrentEntries, "36");
        BigDecimal previousRetainedBase = sumEquityComponentValue(safePreviousEntries, "36");

        BigDecimal currentDividends = sumByMatcher(safeCurrentEntries, financialPositionEntryClassifier::isDividendEntry);
        BigDecimal previousDividends = sumByMatcher(safePreviousEntries, financialPositionEntryClassifier::isDividendEntry);

        BigDecimal currentTreasuryShares = sumByMatcher(safeCurrentEntries, financialPositionEntryClassifier::isTreasuryShareEntry);
        BigDecimal previousTreasuryShares = sumByMatcher(safePreviousEntries, financialPositionEntryClassifier::isTreasuryShareEntry);

        BigDecimal currentSharePremium = sumByMatcher(safeCurrentEntries, financialPositionEntryClassifier::isSharePremiumEntry);
        BigDecimal previousSharePremium = sumByMatcher(safePreviousEntries, financialPositionEntryClassifier::isSharePremiumEntry);

        BigDecimal currentNetIncome = periodResultCalculator.resolveResultForCutoff(safeCurrentEntries, currentCutoffDate);
        BigDecimal previousNetIncome = periodResultCalculator.resolveResultForCutoff(safePreviousEntries, previousCutoffDate);

        BigDecimal currentCapital = scaleAmount(currentCapitalBase.add(currentSharePremium));
        BigDecimal previousCapital = scaleAmount(previousCapitalBase.add(previousSharePremium));

        BigDecimal currentRetainedEarnings = scaleAmount(
                currentRetainedBase
                        .subtract(currentDividends)
                        .subtract(currentTreasuryShares)
        );
        BigDecimal previousRetainedEarnings = scaleAmount(
                previousRetainedBase
                        .subtract(previousDividends)
                        .subtract(previousTreasuryShares)
        );

        BigDecimal currentTotalEquity = scaleAmount(
                currentCapital
                        .add(currentReserves)
                        .add(currentRetainedEarnings)
                        .add(currentNetIncome)
        );
        BigDecimal previousTotalEquity = scaleAmount(
                previousCapital
                        .add(previousReserves)
                        .add(previousRetainedEarnings)
                        .add(previousNetIncome)
        );

        return new EquityChangesAmounts(
                comparative(currentCapital, previousCapital),
                comparative(currentNetIncome, previousNetIncome),
                comparative(currentRetainedEarnings, previousRetainedEarnings),
                comparative(currentReserves, previousReserves),
                comparative(currentTotalEquity, previousTotalEquity),
                buildYearSnapshots(safeSourceEntries, currentCutoffDate, previousCutoffDate)
        );
    }

    private Map<Integer, EquityYearAmounts> buildYearSnapshots(
            List<AccountingEntry> sourceEntries,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        Map<Integer, EquityYearAmounts> snapshots = new LinkedHashMap<>();
        if (currentCutoffDate == null && previousCutoffDate == null) {
            return snapshots;
        }

        int startYear = previousCutoffDate != null
                ? previousCutoffDate.getYear()
                : currentCutoffDate.minusYears(1).getYear();
        int endYear = currentCutoffDate != null
                ? currentCutoffDate.getYear()
                : previousCutoffDate.plusYears(1).getYear();

        if (startYear > endYear) {
            int temp = startYear;
            startYear = endYear;
            endYear = temp;
        }

        for (int year = startYear; year <= endYear; year++) {
            LocalDate yearCutoffDate = resolveYearCutoffDate(
                    year,
                    startYear,
                    endYear,
                    currentCutoffDate,
                    previousCutoffDate
            );

            List<AccountingEntry> entriesUpToCutoff = filterEntriesUpToCutoff(sourceEntries, yearCutoffDate);
            BigDecimal capitalBase = sumEquityComponentValue(entriesUpToCutoff, "31");
            BigDecimal reserves = sumEquityComponentValue(entriesUpToCutoff, "33");
            BigDecimal retainedBase = sumEquityComponentValue(entriesUpToCutoff, "36");
            BigDecimal dividends = sumByMatcher(entriesUpToCutoff, financialPositionEntryClassifier::isDividendEntry);
            BigDecimal treasuryShares = sumByMatcher(entriesUpToCutoff, financialPositionEntryClassifier::isTreasuryShareEntry);
            BigDecimal sharePremium = sumByMatcher(entriesUpToCutoff, financialPositionEntryClassifier::isSharePremiumEntry);
            BigDecimal capital = scaleAmount(capitalBase.add(sharePremium));
            BigDecimal retainedEarnings = scaleAmount(
                    retainedBase
                            .subtract(dividends)
                            .subtract(treasuryShares)
            );
            BigDecimal netIncome = periodResultCalculator.resolveResultForCutoff(entriesUpToCutoff, yearCutoffDate);
            BigDecimal totalEquity = scaleAmount(capital.add(reserves).add(retainedEarnings).add(netIncome));

            snapshots.put(
                    year,
                    new EquityYearAmounts(
                            capital,
                            netIncome,
                            retainedEarnings,
                            reserves,
                            totalEquity
                    )
            );
        }

        return snapshots;
    }

    private LocalDate resolveYearCutoffDate(
            int year,
            int startYear,
            int endYear,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        if (previousCutoffDate != null && year == startYear) {
            return previousCutoffDate;
        }
        if (currentCutoffDate != null && year == endYear) {
            return currentCutoffDate;
        }
        return LocalDate.of(year, 12, 31);
    }

    private BigDecimal sumEquityComponentValue(List<AccountingEntry> entries, String accountPrefix) {
        if (entries == null || entries.isEmpty() || !StringUtils.hasText(accountPrefix)) {
            return scaleAmount(BigDecimal.ZERO);
        }

        return scaleAmount(entries.stream()
                .filter(entry -> accountingEntryOperations.codeStartsWith(entry, accountPrefix))
                .map(accountingEntryOperations::signedAmountByNature)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumByMatcher(List<AccountingEntry> entries, Predicate<AccountingEntry> matcher) {
        if (entries == null || entries.isEmpty() || matcher == null) {
            return scaleAmount(BigDecimal.ZERO);
        }

        return scaleAmount(entries.stream()
                .filter(entry -> entry != null && matcher.test(entry))
                .map(accountingEntryOperations::signedAmountByNature)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private List<AccountingEntry> filterEntriesUpToCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return List.of();
        }

        return entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null && !entry.getDate().isAfter(cutoffDate))
                .toList();
    }

    private ComparativeAmount comparative(BigDecimal current, BigDecimal previous) {
        return new ComparativeAmount(scaleAmount(current), scaleAmount(previous));
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
