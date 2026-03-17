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
import java.util.Objects;
import java.util.function.Predicate;

@Component
public class IncomeStatementAmountCalculator {

    private final AccountingEntryOperations accountingEntryOperations;

    public IncomeStatementAmountCalculator(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public BigDecimal sumEntries(
            List<AccountingEntry> entries,
            Predicate<AccountingEntry> matcher,
            boolean absolute
    ) {
        if (entries == null || entries.isEmpty() || matcher == null) {
            return scaleAmount(BigDecimal.ZERO);
        }

        BigDecimal total = entries.stream()
                .filter(Objects::nonNull)
                .filter(matcher)
                .map(this::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return scaleAmount(absolute ? total.abs() : total);
    }

    public Map<String, BigDecimal> aggregateByAccountPrefix(List<AccountingEntry> entries, String accountPrefix) {
        Map<String, BigDecimal> amountByAccount = new LinkedHashMap<>();
        if (entries == null || entries.isEmpty() || !StringUtils.hasText(accountPrefix)) {
            return amountByAccount;
        }

        for (AccountingEntry entry : entries) {
            if (!accountingEntryOperations.codeStartsWith(entry, accountPrefix)) {
                continue;
            }

            String accountCode = accountingEntryOperations.resolveAccountCode(entry);
            String accountName = StringUtils.hasText(entry.getAccountName()) ? entry.getAccountName() : "Cuenta " + accountCode;
            String accountKey = accountCode + "|" + accountName;

            amountByAccount.merge(accountKey, signedAmount(entry), BigDecimal::add);
        }

        amountByAccount.replaceAll((key, amount) -> scaleAmount(amount));
        return amountByAccount;
    }

    public BigDecimal sumAmounts(Map<String, BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return scaleAmount(BigDecimal.ZERO);
        }
        return scaleAmount(values.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal calculateNetIncomeForCutoff(List<AccountingEntry> entries, LocalDate cutoffDate) {
        if (entries == null || entries.isEmpty() || cutoffDate == null) {
            return scaleAmount(BigDecimal.ZERO);
        }

        LocalDate periodStart = cutoffDate.withDayOfYear(1);
        List<AccountingEntry> periodEntries = entries.stream()
                .filter(entry -> entry != null && entry.getDate() != null)
                .filter(entry -> !entry.getDate().isBefore(periodStart) && !entry.getDate().isAfter(cutoffDate))
                .toList();

        BigDecimal totalIncome = sumAmounts(aggregateByAccountPrefix(periodEntries, "4"));
        BigDecimal totalCost = sumAmounts(aggregateByAccountPrefix(periodEntries, "6"));
        BigDecimal totalExpense = sumAmounts(aggregateByAccountPrefix(periodEntries, "5"));

        return scaleAmount(totalIncome.subtract(totalCost).subtract(totalExpense));
    }

    public BigDecimal signedAmount(AccountingEntry entry) {
        String accountCode = accountingEntryOperations.resolveAccountCode(entry);
        if (accountCode == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal debit = safeAmount(entry.getDebit());
        BigDecimal credit = safeAmount(entry.getCredit());

        if (accountCode.startsWith("4")) {
            return credit.subtract(debit);
        }

        if (accountCode.startsWith("5") || accountCode.startsWith("6")) {
            return debit.subtract(credit);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
