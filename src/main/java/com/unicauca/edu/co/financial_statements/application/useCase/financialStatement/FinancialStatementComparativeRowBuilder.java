package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementCriteriaType;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class FinancialStatementComparativeRowBuilder {

    private final AccountingEntryOperations accountingEntryOperations;

    public FinancialStatementComparativeRowBuilder(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public Map<String, Object> buildComparativeRow(
            String lineDescription,
            String note,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            String rowType,
            BigDecimal currentPercentageBase,
            BigDecimal previousPercentageBase
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        BigDecimal variation = currentAmount != null && previousAmount != null
                ? scaleAmount(currentAmount.subtract(previousAmount))
                : null;
        BigDecimal currentPercentage = calculateParticipationPercentage(currentAmount, currentPercentageBase);
        BigDecimal previousPercentage = calculateParticipationPercentage(previousAmount, previousPercentageBase);

        row.put("lineDescription", lineDescription);
        row.put("note", note);
        row.put("currentAmount", currentAmount);
        row.put("currentPercentage", currentPercentage);
        row.put("previousAmount", previousAmount);
        row.put("previousPercentage", previousPercentage);
        row.put("variation", variation);
        row.put("variationPercentage", calculateVariationPercentage(currentPercentage, previousPercentage));
        row.put("rowType", rowType);
        return row;
    }

    public Map<String, Object> buildAccount(String code, String description, String nature) {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("accountCode", code);
        account.put("accountDescription", description);
        account.put("nature", nature);
        return account;
    }

    public List<Map<String, Object>> buildLevelComparisonRows(
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            FinancialStatementCriteria criteria,
            Predicate<AccountingEntry> matcher,
            BigDecimal currentPercentageBase,
            BigDecimal previousPercentageBase,
            Function<AccountingEntry, BigDecimal> amountResolver,
            boolean absoluteAmounts
    ) {
        if (!hasLevelCriteria(criteria) || matcher == null || amountResolver == null) {
            return List.of();
        }

        int prefixLength = EFinancialStatementCriteriaType.resolvePrefixLength(criteria.getCriteriaType());
        if (prefixLength <= 0) {
            return List.of();
        }

        Map<String, LevelAggregation> currentAggregation = aggregateEntriesByLevel(
                currentEntries,
                matcher,
                prefixLength,
                amountResolver,
                absoluteAmounts
        );
        Map<String, LevelAggregation> previousAggregation = aggregateEntriesByLevel(
                previousEntries,
                matcher,
                prefixLength,
                amountResolver,
                absoluteAmounts
        );

        Set<String> orderedCodes = new LinkedHashSet<>();
        orderedCodes.addAll(currentAggregation.keySet());
        orderedCodes.addAll(previousAggregation.keySet());

        return orderedCodes.stream()
                .sorted(Comparator.comparingLong(accountingEntryOperations::parseSortableAccountCode))
                .map(accountCode -> {
                    LevelAggregation current = currentAggregation.get(accountCode);
                    LevelAggregation previous = previousAggregation.get(accountCode);
                    BigDecimal currentAmount = current != null ? current.amount() : null;
                    BigDecimal previousAmount = previous != null ? previous.amount() : null;

                    boolean emptyCurrent = currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) == 0;
                    boolean emptyPrevious = previousAmount == null || previousAmount.compareTo(BigDecimal.ZERO) == 0;
                    if (emptyCurrent && emptyPrevious) {
                        return null;
                    }

                    String description = resolveGroupedAccountDescription(
                            accountCode,
                            current != null ? current.accountNames() : List.of(),
                            previous != null ? previous.accountNames() : List.of()
                    );
                    String nature = current != null && StringUtils.hasText(current.nature())
                            ? current.nature()
                            : (previous != null ? previous.nature() : "DEBITO");

                    Map<String, Object> row = buildComparativeRow(
                            "  " + accountCode + " - " + description,
                            null,
                            currentAmount != null ? scaleAmount(currentAmount) : null,
                            previousAmount != null ? scaleAmount(previousAmount) : null,
                            "DETAIL",
                            currentPercentageBase,
                            previousPercentageBase
                    );
                    row.put("account", buildAccount(accountCode, description, nature));
                    row.put("accountCode", accountCode);
                    row.put("accountDescription", description);
                    return row;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, LevelAggregation> aggregateEntriesByLevel(
            List<AccountingEntry> entries,
            Predicate<AccountingEntry> matcher,
            int prefixLength,
            Function<AccountingEntry, BigDecimal> amountResolver,
            boolean absoluteAmounts
    ) {
        Map<String, BigDecimal> amountByCode = new LinkedHashMap<>();
        Map<String, List<String>> namesByCode = new LinkedHashMap<>();
        Map<String, String> natureByCode = new LinkedHashMap<>();

        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }

        for (AccountingEntry entry : entries) {
            if (entry == null || !matcher.test(entry)) {
                continue;
            }

            String projectedCode = accountingEntryOperations.resolveProjectedAccountCode(entry, prefixLength);
            if (!StringUtils.hasText(projectedCode)) {
                continue;
            }

            amountByCode.merge(projectedCode, amountResolver.apply(entry), BigDecimal::add);

            if (StringUtils.hasText(entry.getAccountName())) {
                namesByCode.computeIfAbsent(projectedCode, ignored -> new ArrayList<>())
                        .add(entry.getAccountName().trim());
            }
            natureByCode.putIfAbsent(projectedCode, accountingEntryOperations.normalizeNature(entry.getAccountNature()));
        }

        Map<String, LevelAggregation> aggregation = new LinkedHashMap<>();
        amountByCode.forEach((code, amount) -> {
            BigDecimal resolvedAmount = scaleAmount(absoluteAmounts ? amount.abs() : amount);
            aggregation.put(
                    code,
                    new LevelAggregation(
                            resolvedAmount,
                            namesByCode.getOrDefault(code, List.of()),
                            natureByCode.getOrDefault(code, "DEBITO")
                    )
            );
        });
        return aggregation;
    }

    private boolean hasLevelCriteria(FinancialStatementCriteria criteria) {
        return criteria != null && StringUtils.hasText(criteria.getCriteriaType());
    }

    private String resolveGroupedAccountDescription(
            String accountCode,
            List<String> currentNames,
            List<String> previousNames
    ) {
        List<String> accountNames = new ArrayList<>();
        accountNames.addAll(currentNames != null ? currentNames : List.of());
        accountNames.addAll(previousNames != null ? previousNames : List.of());

        List<String> distinctNames = accountNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        if (distinctNames.isEmpty()) {
            return "Cuenta " + accountCode;
        }

        if (distinctNames.size() == 1) {
            return distinctNames.get(0);
        }

        String sharedLeadingSegment = resolveSharedLeadingAccountSegment(distinctNames);
        if (StringUtils.hasText(sharedLeadingSegment)) {
            return sharedLeadingSegment;
        }

        return distinctNames.stream()
                .min(Comparator.comparingInt(String::length))
                .orElse("Cuenta " + accountCode);
    }

    private String resolveSharedLeadingAccountSegment(List<String> accountNames) {
        if (accountNames == null || accountNames.isEmpty()) {
            return null;
        }

        List<String> leadingSegments = accountNames.stream()
                .map(this::extractLeadingAccountSegment)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        if (leadingSegments.size() == 1) {
            return leadingSegments.get(0);
        }

        return null;
    }

    private String extractLeadingAccountSegment(String accountName) {
        if (!StringUtils.hasText(accountName)) {
            return null;
        }

        String normalized = accountName.trim();
        String[] separators = {" - ", " â€“ ", ":"};
        for (String separator : separators) {
            int separatorIndex = normalized.indexOf(separator);
            if (separatorIndex > 0) {
                return normalized.substring(0, separatorIndex).trim();
            }
        }

        return normalized;
    }

    private BigDecimal calculateParticipationPercentage(BigDecimal amount, BigDecimal totalAssets) {
        if (amount == null || totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return scaleAmount(amount
                .multiply(new BigDecimal("100"))
                .divide(totalAssets, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateVariationPercentage(BigDecimal currentPercentage, BigDecimal previousPercentage) {
        if (currentPercentage == null || previousPercentage == null) {
            return null;
        }

        return scaleAmount(currentPercentage.subtract(previousPercentage));
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private record LevelAggregation(
            BigDecimal amount,
            List<String> accountNames,
            String nature
    ) {
    }
}
