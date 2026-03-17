package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.out.IAccountingInfoClient;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteriaRange;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
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
public class FinancialStatementDataGenerator {

    private final IAccountingInfoClient accountingInfoClient;
    private final AccountingEntryOperations accountingEntryOperations;
    private final IncomeStatementStatementBuilder incomeStatementStatementBuilder;
    private final FinancialPositionStatementBuilder financialPositionStatementBuilder;
    private final EquityChangesStatementBuilder equityChangesStatementBuilder;
    private final FinancialStatementRowMapper financialStatementRowMapper;

    public FinancialStatementDataPayload generate(FinancialStatementRequest request) {
        FinancialStatementCriteria criteria = request.getCriteria();
        LocalDate startDate = criteria != null ? criteria.getStartDate() : null;
        LocalDate endDate = criteria != null ? criteria.getEndDate() : null;
        LocalDate previousStartDate = criteria != null ? criteria.getPreviousStartDate() : null;
        LocalDate previousEndDate = criteria != null ? criteria.getPreviousEndDate() : null;
        LocalDate currentCutoffDate = criteria != null && criteria.getCurrentCutoffDate() != null
                ? criteria.getCurrentCutoffDate()
                : endDate;
        LocalDate previousCutoffDate = criteria != null && criteria.getPreviousCutoffDate() != null
                ? criteria.getPreviousCutoffDate()
                : startDate;

        return switch (request.getType()) {
            case INCOME_STATEMENT -> buildIncomeStatementData(
                    request.getEntId(),
                    criteria,
                    startDate,
                    endDate,
                    previousStartDate,
                    previousEndDate
            );
            case STATEMENT_CHANGES_EQUITY -> buildEquityChangesData(
                    request.getEntId(),
                    endDate,
                    startDate
            );
            case STATEMENT_FINANCIAL_POSITION -> buildFinancialPositionData(
                    request.getEntId(),
                    criteria,
                    currentCutoffDate,
                    previousCutoffDate
            );
        };
    }

    private FinancialStatementDataPayload buildIncomeStatementData(
            String enterpriseId,
            FinancialStatementCriteria criteria,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate previousStartDate,
            LocalDate previousEndDate
    ) {
        List<AccountingEntry> accountingEntries = accountingInfoClient.findAccountingEntries(enterpriseId, startDate, endDate);
        List<AccountingEntry> filteredAccountingEntries = applyCriteriaLevelFilter(accountingEntries, criteria, false);
        LocalDate resolvedPreviousStartDate = previousStartDate != null
                ? previousStartDate
                : (startDate != null ? startDate.minusYears(1) : null);
        LocalDate resolvedPreviousEndDate = previousEndDate != null
                ? previousEndDate
                : (endDate != null ? endDate.minusYears(1) : null);
        List<AccountingEntry> previousAccountingEntries = accountingInfoClient.findAccountingEntries(
                enterpriseId,
                resolvedPreviousStartDate,
                resolvedPreviousEndDate
        );
        List<AccountingEntry> filteredPreviousAccountingEntries = applyCriteriaLevelFilter(previousAccountingEntries, criteria, false);

        return FinancialStatementDataPayload.builder()
                .rows(financialStatementRowMapper.toTypedRows(
                        incomeStatementStatementBuilder.buildRows(
                                filteredAccountingEntries,
                                filteredPreviousAccountingEntries,
                                criteria
                        )
                ))
                .build();
    }

    private FinancialStatementDataPayload buildEquityChangesData(
            String enterpriseId,
            LocalDate endDate,
            LocalDate startDate
    ) {
        List<AccountingEntry> sourceEntries = accountingInfoClient.findAccountingEntries(enterpriseId, null, endDate);
        List<AccountingEntry> currentAccountingEntries = filterEntriesUpToCutoff(sourceEntries, endDate);
        List<AccountingEntry> previousAccountingEntries = filterEntriesUpToCutoff(sourceEntries, startDate);

        return FinancialStatementDataPayload.builder()
                .rows(financialStatementRowMapper.toTypedRows(
                        equityChangesStatementBuilder.buildRows(
                                sourceEntries,
                                currentAccountingEntries,
                                previousAccountingEntries,
                                endDate,
                                startDate
                        )
                ))
                .build();
    }

    private FinancialStatementDataPayload buildFinancialPositionData(
            String enterpriseId,
            FinancialStatementCriteria criteria,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        List<AccountingEntry> sourceEntries = accountingInfoClient.findAccountingEntries(enterpriseId, null, currentCutoffDate);
        List<AccountingEntry> filteredSourceEntries = applyCriteriaLevelFilter(sourceEntries, criteria, true);

        LocalDate latestMovementDate = filteredSourceEntries.stream()
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

        List<AccountingEntry> currentAccountingEntries = filterEntriesUpToCutoff(filteredSourceEntries, currentCutoffDate);
        List<AccountingEntry> previousAccountingEntries = filterEntriesUpToCutoff(filteredSourceEntries, previousCutoffDate);

        FinancialPositionRowsResult result = financialPositionStatementBuilder.build(
                currentAccountingEntries,
                previousAccountingEntries,
                criteria,
                currentCutoffDate,
                previousCutoffDate
        );

        validateBalancedFinancialPosition(
                result.totalAssets(),
                result.totalLiabilities(),
                result.totalEquity(),
                currentCutoffDate,
                "current"
        );
        validateBalancedFinancialPosition(
                result.previousTotalAssets(),
                result.previousTotalLiabilities(),
                result.previousTotalEquity(),
                previousCutoffDate,
                "previous"
        );

        return FinancialStatementDataPayload.builder()
                .rows(financialStatementRowMapper.toTypedRows(result.rows()))
                .totalAssets(result.totalAssets())
                .totalLiabilities(result.totalLiabilities())
                .totalEquity(result.totalEquity())
                .build();
    }

    private List<AccountingEntry> filterEntriesUpToCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return List.of();
        }

        return entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null && !entry.getDate().isAfter(cutoffDate))
                .toList();
    }

    private List<AccountingEntry> applyCriteriaLevelFilter(
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

    private void validateBalancedFinancialPosition(
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
