package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class AccountingEntryOperations {

    private static final int CLASS_PREFIX_LENGTH = 1;
    private static final int GROUP_PREFIX_LENGTH = 2;
    private static final int ACCOUNT_PREFIX_LENGTH = 4;
    private static final int SUB_ACCOUNT_PREFIX_LENGTH = 6;
    private static final int AUXILIARY_PREFIX_LENGTH = 8;

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

    public boolean codeStartsWithAny(AccountingEntry entry, String... prefixes) {
        if (prefixes == null || prefixes.length == 0) {
            return false;
        }
        return Arrays.stream(prefixes)
                .filter(StringUtils::hasText)
                .anyMatch(prefix -> codeStartsWith(entry, prefix));
    }

    public boolean matchesAccountingClass(AccountingEntry entry, String classCode) {
        return matchesAtLevel(entry, classCode, CLASS_PREFIX_LENGTH);
    }

    public boolean matchesGroup(AccountingEntry entry, String groupCode) {
        return matchesAtLevel(entry, groupCode, GROUP_PREFIX_LENGTH);
    }

    public boolean matchesAccount(AccountingEntry entry, String accountCode) {
        return matchesAtLevel(entry, accountCode, ACCOUNT_PREFIX_LENGTH);
    }

    public boolean matchesSubAccount(AccountingEntry entry, String subAccountCode) {
        return matchesAtLevel(entry, subAccountCode, SUB_ACCOUNT_PREFIX_LENGTH);
    }

    public boolean matchesAuxiliary(AccountingEntry entry, String auxiliaryCode) {
        return matchesAtLevel(entry, auxiliaryCode, AUXILIARY_PREFIX_LENGTH);
    }

    public boolean matchesAnyGroup(AccountingEntry entry, String... groupCodes) {
        return matchesAnyAtLevel(entry, GROUP_PREFIX_LENGTH, groupCodes);
    }

    public boolean matchesAnyAccount(AccountingEntry entry, String... accountCodes) {
        return matchesAnyAtLevel(entry, ACCOUNT_PREFIX_LENGTH, accountCodes);
    }

    public boolean matchesAnySubAccount(AccountingEntry entry, String... subAccountCodes) {
        return matchesAnyAtLevel(entry, SUB_ACCOUNT_PREFIX_LENGTH, subAccountCodes);
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

    private boolean matchesAtLevel(AccountingEntry entry, String code, int levelLength) {
        if (!StringUtils.hasText(code) || levelLength <= 0) {
            return false;
        }

        String normalizedCode = normalizeComparableCode(code, levelLength);
        if (normalizedCode == null) {
            return false;
        }

        String projectedCode = resolveProjectedAccountCode(entry, levelLength);
        return normalizedCode.equals(projectedCode);
    }

    private boolean matchesAnyAtLevel(AccountingEntry entry, int levelLength, String... codes) {
        if (codes == null || codes.length == 0) {
            return false;
        }

        List<String> normalizedCodes = Arrays.stream(codes)
                .map(code -> normalizeComparableCode(code, levelLength))
                .filter(StringUtils::hasText)
                .toList();

        if (normalizedCodes.isEmpty()) {
            return false;
        }

        String projectedCode = resolveProjectedAccountCode(entry, levelLength);
        return StringUtils.hasText(projectedCode) && normalizedCodes.contains(projectedCode);
    }

    private String normalizeComparableCode(String code, int levelLength) {
        if (!StringUtils.hasText(code) || levelLength <= 0) {
            return null;
        }

        String normalizedCode = code.trim();
        int effectiveLength = Math.min(levelLength, normalizedCode.length());
        return normalizedCode.substring(0, effectiveLength);
    }
}
