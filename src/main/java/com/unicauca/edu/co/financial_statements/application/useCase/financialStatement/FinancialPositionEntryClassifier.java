package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FinancialPositionEntryClassifier {

    private final AccountingEntryOperations accountingEntryOperations;

    public FinancialPositionEntryClassifier(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public boolean isCashAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && codeStartsWith(entry, "11");
    }

    public boolean isReceivableAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && codeStartsWith(entry, "13") && !codeStartsWith(entry, "1355");
    }

    public boolean isTemporaryFinancialAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && nameContains(entry, "temporal");
    }

    public boolean isInventoryAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && codeStartsWith(entry, "14") && !nameContains(entry, "biologic");
    }

    public boolean isCurrentTaxAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && (codeStartsWith(entry, "1355") || nameContainsAllTokens(entry, "impuesto", "corriente"));
    }

    public boolean isBiologicalAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && nameContains(entry, "biologic");
    }

    public boolean isHeldForSaleAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && (codeStartsWith(entry, "18") || nameContainsAllTokens(entry, "mantenidos", "venta"));
    }

    public boolean isPropertyPlantEquipmentEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && codeStartsWith(entry, "15") && !nameContains(entry, "inversion");
    }

    public boolean isPermanentFinancialAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && (nameContainsAllTokens(entry, "financier", "permanent")
                || nameContainsAllTokens(entry, "inversion", "perman"));
    }

    public boolean isIntangibleAssetEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry) && (codeStartsWith(entry, "16") || nameContains(entry, "intangible"));
    }

    public boolean isInvestmentPropertyEntry(AccountingEntry entry) {
        return isAssetClassEntry(entry)
                && (nameContains(entry, "propiedades de inversion")
                || nameContainsAllTokens(entry, "propiedad", "inversion"));
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
                && (nameContainsAny(entry, "proveedor", "acreedor", "cuentas por pagar")
                || (codeStartsWith(entry, "22") && !isFinancialObligation(entry)));
    }

    public boolean isCurrentTaxLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry)
                && (codeStartsWith(entry, "24")
                || nameContainsAllTokens(entry, "impuestos", "corrientes")
                || nameContainsAllTokens(entry, "impuesto", "corriente"));
    }

    public boolean isProvisionLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry) && (codeStartsWith(entry, "26") || nameContains(entry, "provision"));
    }

    public boolean isLongTermFinancialLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry)
                && (codeStartsWith(entry, "23")
                || (isFinancialObligation(entry)
                && (nameContainsAllTokens(entry, "financier", "largo", "plazo")
                || nameContains(entry, "no corriente"))));
    }

    public boolean isCapitalEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && codeStartsWith(entry, "31");
    }

    public boolean isReserveEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && codeStartsWith(entry, "33");
    }

    public boolean isRetainedEarningsEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && codeStartsWith(entry, "36");
    }

    public boolean isDividendEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry)
                && (codeStartsWith(entry, "37") || nameContainsAllTokens(entry, "dividendo", "decret"));
    }

    public boolean isTreasuryShareEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && nameContainsAllTokens(entry, "acciones", "readquir");
    }

    public boolean isSharePremiumEntry(AccountingEntry entry) {
        return isEquityClassEntry(entry) && nameContainsAllTokens(entry, "prima", "emision");
    }

    public boolean isDeferredTaxLiabilityEntry(AccountingEntry entry) {
        return isLiabilityClassEntry(entry) && (codeStartsWith(entry, "27") || nameContains(entry, "diferid"));
    }

    public boolean isCurrentFinancialLiability(AccountingEntry entry) {
        if (!isLiabilityClassEntry(entry)) {
            return false;
        }

        boolean financialName = isFinancialObligation(entry);
        boolean currentLiabilityCode = codeStartsWith(entry, "21");
        boolean longTermCode = codeStartsWith(entry, "23");
        boolean longTermName = nameContainsAllTokens(entry, "largo", "plazo") || nameContains(entry, "no corriente");

        return (financialName || currentLiabilityCode)
                && !longTermCode
                && !longTermName
                && !nameContainsAny(entry, "proveedor", "acreedor", "cuentas por pagar");
    }

    private boolean isAssetClassEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "1");
    }

    private boolean isLiabilityClassEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "2");
    }

    private boolean isEquityClassEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "3");
    }

    private boolean codeStartsWith(AccountingEntry entry, String prefix) {
        return accountingEntryOperations.codeStartsWith(entry, prefix);
    }

    private boolean isFinancialObligation(AccountingEntry entry) {
        return nameContainsAny(entry, "obligaciones financieras", "pasivos financieros", "credito bancario", "prestamo");
    }

    private boolean nameContainsAny(AccountingEntry entry, String... tokens) {
        if (entry == null || !StringUtils.hasText(entry.getAccountName()) || tokens == null || tokens.length == 0) {
            return false;
        }

        String normalizedName = entry.getAccountName().toLowerCase();
        for (String token : tokens) {
            if (StringUtils.hasText(token) && normalizedName.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean nameContains(AccountingEntry entry, String token) {
        if (entry == null || !StringUtils.hasText(entry.getAccountName()) || !StringUtils.hasText(token)) {
            return false;
        }
        return entry.getAccountName().toLowerCase().contains(token.toLowerCase());
    }

    private boolean nameContainsAllTokens(AccountingEntry entry, String... tokens) {
        if (entry == null || !StringUtils.hasText(entry.getAccountName()) || tokens == null || tokens.length == 0) {
            return false;
        }

        String name = entry.getAccountName().toLowerCase();
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            if (!name.contains(token.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
