package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FinancialPositionEntryClassifier {

    private static final String[] TEMPORARY_FINANCIAL_ASSET_ACCOUNTS = {"1205", "1206", "1207", "1208"};
    private static final String[] PAYABLE_NAME_FALLBACK_TOKENS = {"proveedor", "acreedor", "cuentas por pagar"};
    private static final String[] FINANCIAL_NAME_FALLBACK_TOKENS = {
            "obligaciones financieras",
            "pasivos financieros",
            "credito bancario",
            "prestamo"
    };
    private static final String[] LONG_TERM_NAME_FALLBACK_TOKENS = {"largo plazo", "no corriente"};

    private final AccountingEntryOperations accountingEntryOperations;

    public FinancialPositionEntryClassifier(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public boolean isCashAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "11");
    }

    public boolean isReceivableAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && accountingEntryOperations.matchesGroup(entry, "13")
                && !isCurrentTaxAssetEntry(entry);
    }

    public boolean isTemporaryFinancialAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && (accountingEntryOperations.matchesAnyAccount(entry, TEMPORARY_FINANCIAL_ASSET_ACCOUNTS)
                || (accountingEntryOperations.matchesGroup(entry, "12") && nameContainsAny(entry, "temporal", "corto plazo")));
    }

    public boolean isInventoryAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && accountingEntryOperations.matchesGroup(entry, "14")
                && !isBiologicalAssetEntry(entry);
    }

    public boolean isCurrentTaxAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesAccount(entry, "1355");
    }

    public boolean isBiologicalAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesAccount(entry, "1465");
    }

    public boolean isHeldForSaleAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "18");
    }

    public boolean isPropertyPlantEquipmentEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && accountingEntryOperations.matchesGroup(entry, "15")
                && !isInvestmentPropertyEntry(entry);
    }

    public boolean isPermanentFinancialAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && accountingEntryOperations.matchesGroup(entry, "12")
                && !isTemporaryFinancialAssetEntry(entry);
    }

    public boolean isIntangibleAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "16");
    }

    public boolean isInvestmentPropertyEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && accountingEntryOperations.matchesAccount(entry, "1516");
    }

    public boolean isOtherAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && !isCashAssetEntry(entry)
                && !isReceivableAssetEntry(entry)
                && !isTemporaryFinancialAssetEntry(entry)
                && !isInventoryAssetEntry(entry)
                && !isCurrentTaxAssetEntry(entry)
                && !isBiologicalAssetEntry(entry)
                && !isHeldForSaleAssetEntry(entry)
                && !isPropertyPlantEquipmentEntry(entry)
                && !isPermanentFinancialAssetEntry(entry)
                && !isIntangibleAssetEntry(entry)
                && !isInvestmentPropertyEntry(entry);
    }

    public boolean isTradePayableEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry)
                && !isCurrentTaxLiabilityEntry(entry)
                && !isProvisionLiabilityEntry(entry)
                && !isDeferredTaxLiabilityEntry(entry)
                && !isLongTermFinancialLiabilityEntry(entry)
                && !isCurrentFinancialLiability(entry)
                && (accountingEntryOperations.matchesGroup(entry, "22") || hasTradePayableNameFallback(entry));
    }

    public boolean isCurrentTaxLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "24");
    }

    public boolean isProvisionLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "26");
    }

    public boolean isLongTermFinancialLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry)
                && (accountingEntryOperations.matchesGroup(entry, "23")
                || (accountingEntryOperations.matchesAnyGroup(entry, "21", "22")
                && hasFinancialLiabilityNameFallback(entry)
                && hasLongTermNameFallback(entry)));
    }

    public boolean isCapitalEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "31");
    }

    public boolean isReserveEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "33");
    }

    public boolean isRetainedEarningsEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "36");
    }

    public boolean isDividendEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "37");
    }

    public boolean isTreasuryShareEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry)
                && (accountingEntryOperations.matchesSubAccount(entry, "320505")
                || (accountingEntryOperations.matchesAccount(entry, "3205")
                && nameContainsAllTokens(entry, "acciones", "readquir")));
    }

    public boolean isSharePremiumEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry)
                && (accountingEntryOperations.matchesSubAccount(entry, "320510")
                || (accountingEntryOperations.matchesAccount(entry, "3205")
                && nameContainsAllTokens(entry, "prima", "emision")));
    }

    public boolean isDeferredTaxLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry) && accountingEntryOperations.matchesGroup(entry, "27");
    }

    public boolean isCurrentFinancialLiability(AccountingEntry entry) {
        return isLiabilityClassEntry(entry)
                && !isCurrentTaxLiabilityEntry(entry)
                && !isProvisionLiabilityEntry(entry)
                && !isDeferredTaxLiabilityEntry(entry)
                && !isLongTermFinancialLiabilityEntry(entry)
                && !hasTradePayableNameFallback(entry)
                && (accountingEntryOperations.matchesGroup(entry, "21")
                || (accountingEntryOperations.matchesGroup(entry, "22")
                && hasFinancialLiabilityNameFallback(entry)
                && !hasLongTermNameFallback(entry)));
    }

    private boolean isAssetClassEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "1");
    }

    private boolean isLiabilityClassEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "2");
    }

    private boolean isEquityClassEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "3");
    }

    private boolean hasTradePayableNameFallback(AccountingEntry entry) {
        return nameContainsAny(entry, PAYABLE_NAME_FALLBACK_TOKENS);
    }

    private boolean hasFinancialLiabilityNameFallback(AccountingEntry entry) {
        return nameContainsAny(entry, FINANCIAL_NAME_FALLBACK_TOKENS);
    }

    private boolean hasLongTermNameFallback(AccountingEntry entry) {
        return nameContainsAny(entry, LONG_TERM_NAME_FALLBACK_TOKENS);
    }

    private boolean nameContainsAny(AccountingEntry entry, String... tokens) {
        String normalizedName = normalizeName(entry);
        if (!StringUtils.hasText(normalizedName) || tokens == null || tokens.length == 0) {
            return false;
        }

        for (String token : tokens) {
            if (StringUtils.hasText(token) && normalizedName.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean nameContainsAllTokens(AccountingEntry entry, String... tokens) {
        String normalizedName = normalizeName(entry);
        if (!StringUtils.hasText(normalizedName) || tokens == null || tokens.length == 0) {
            return false;
        }

        for (String token : tokens) {
            if (StringUtils.hasText(token) && !normalizedName.contains(token.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private String normalizeName(AccountingEntry entry) {
        if (entry == null || !StringUtils.hasText(entry.getAccountName())) {
            return null;
        }
        return entry.getAccountName().toLowerCase();
    }
}
