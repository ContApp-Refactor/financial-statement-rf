package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class AccountingEntryOperations {

    public String resolveAccountCode(AccountingEntry entry) {
        if (entry == null || entry.getAccountCode() == null) {
            return null;
        }

        String accountCode = entry.getAccountCode().trim();
        return StringUtils.hasText(accountCode) ? accountCode : null;
    }

    public boolean codeStartsWith(AccountingEntry entry, String prefix) {
        String accountCode = resolveAccountCode(entry);
        return accountCode != null && StringUtils.hasText(prefix) && accountCode.startsWith(prefix);
    }

    public boolean isIncomeStatementAccount(AccountingEntry entry) {
        String accountCode = resolveAccountCode(entry);
        return accountCode != null
                && (accountCode.startsWith("4")
                || accountCode.startsWith("5")
                || accountCode.startsWith("6"));
    }

    public boolean matchesCriteriaRange(AccountingEntry entry, int prefixLength, long from, long to) {
        String accountCode = resolveAccountCode(entry);
        if (accountCode == null || prefixLength <= 0) {
            return false;
        }

        int effectiveLength = Math.min(prefixLength, accountCode.length());
        String comparablePrefix = accountCode.substring(0, effectiveLength);
        try {
            long comparableValue = Long.parseLong(comparablePrefix);
            return comparableValue >= from && comparableValue <= to;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public String resolveProjectedAccountCode(AccountingEntry entry, int prefixLength) {
        String accountCode = resolveAccountCode(entry);
        if (accountCode == null || prefixLength <= 0) {
            return null;
        }

        int effectiveLength = Math.min(prefixLength, accountCode.length());
        return accountCode.substring(0, effectiveLength);
    }

    public long parseSortableAccountCode(String accountCode) {
        if (!StringUtils.hasText(accountCode)) {
            return Long.MAX_VALUE;
        }

        try {
            return Long.parseLong(accountCode);
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    public String normalizeNature(String nature) {
        if (!StringUtils.hasText(nature)) {
            return "DEBITO";
        }
        return nature.trim().toUpperCase();
    }

    public BigDecimal signedAmountByNature(AccountingEntry entry) {
        String normalizedNature = normalizeNature(entry != null ? entry.getAccountNature() : null);
        BigDecimal debit = safeAmount(entry != null ? entry.getDebit() : null);
        BigDecimal credit = safeAmount(entry != null ? entry.getCredit() : null);

        if ("CREDITO".equalsIgnoreCase(normalizedNature)) {
            return credit.subtract(debit);
        }

        return debit.subtract(credit);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
