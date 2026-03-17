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

@Component
public class EquityChangesAmountCalculator {

    private final AccountingEntryOperations accountingEntryOperations;
    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator;

    public EquityChangesAmountCalculator(
            AccountingEntryOperations accountingEntryOperations,
            IncomeStatementAmountCalculator incomeStatementAmountCalculator
    ) {
        this.accountingEntryOperations = accountingEntryOperations;
        this.incomeStatementAmountCalculator = incomeStatementAmountCalculator;
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

        BigDecimal currentDividends = sumDividendValue(safeCurrentEntries);
        BigDecimal previousDividends = sumDividendValue(safePreviousEntries);

        BigDecimal currentTreasuryShares = sumByNameContainsAll(safeCurrentEntries, "acciones", "readquir");
        BigDecimal previousTreasuryShares = sumByNameContainsAll(safePreviousEntries, "acciones", "readquir");

        BigDecimal currentSharePremium = sumByNameContainsAll(safeCurrentEntries, "prima", "emision");
        BigDecimal previousSharePremium = sumByNameContainsAll(safePreviousEntries, "prima", "emision");

        BigDecimal currentNetIncome = incomeStatementAmountCalculator.calculateNetIncomeForCutoff(safeCurrentEntries, currentCutoffDate);
        BigDecimal previousNetIncome = incomeStatementAmountCalculator.calculateNetIncomeForCutoff(safePreviousEntries, previousCutoffDate);

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
            BigDecimal dividends = sumDividendValue(entriesUpToCutoff);
            BigDecimal treasuryShares = sumByNameContainsAll(entriesUpToCutoff, "acciones", "readquir");
            BigDecimal sharePremium = sumByNameContainsAll(entriesUpToCutoff, "prima", "emision");
            BigDecimal capital = scaleAmount(capitalBase.add(sharePremium));
            BigDecimal retainedEarnings = scaleAmount(
                    retainedBase
                            .subtract(dividends)
                            .subtract(treasuryShares)
            );
            BigDecimal netIncome = incomeStatementAmountCalculator.calculateNetIncomeForCutoff(entriesUpToCutoff, yearCutoffDate);
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

    private BigDecimal sumDividendValue(List<AccountingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return scaleAmount(BigDecimal.ZERO);
        }

        return scaleAmount(entries.stream()
                .filter(entry -> {
                    if (entry == null) {
                        return false;
                    }

                    String code = accountingEntryOperations.resolveAccountCode(entry);
                    String accountName = entry.getAccountName() != null
                            ? entry.getAccountName().toLowerCase()
                            : "";

                    return StringUtils.hasText(code)
                            && (code.startsWith("37")
                            || (code.startsWith("3") && containsAny(accountName, "dividendo", "dividendos")));
                })
                .map(accountingEntryOperations::signedAmountByNature)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumByNameContainsAll(List<AccountingEntry> entries, String... tokens) {
        if (entries == null || entries.isEmpty() || tokens == null || tokens.length == 0) {
            return scaleAmount(BigDecimal.ZERO);
        }

        List<String> normalizedTokens = java.util.Arrays.stream(tokens)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .toList();

        if (normalizedTokens.isEmpty()) {
            return scaleAmount(BigDecimal.ZERO);
        }

        return scaleAmount(entries.stream()
                .filter(entry -> entry != null && StringUtils.hasText(entry.getAccountName()))
                .filter(entry -> {
                    String name = entry.getAccountName().toLowerCase();
                    return normalizedTokens.stream().allMatch(name::contains);
                })
                .map(accountingEntryOperations::signedAmountByNature)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs());
    }

    private List<AccountingEntry> filterEntriesUpToCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return List.of();
        }

        return entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null && !entry.getDate().isAfter(cutoffDate))
                .toList();
    }

    private boolean containsAny(String value, String... tokens) {
        if (!StringUtils.hasText(value) || tokens == null || tokens.length == 0) {
            return false;
        }

        for (String token : tokens) {
            if (StringUtils.hasText(token) && value.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
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
