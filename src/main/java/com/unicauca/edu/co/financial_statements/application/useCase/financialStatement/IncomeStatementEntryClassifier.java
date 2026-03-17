package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IncomeStatementEntryClassifier {

    private final AccountingEntryOperations accountingEntryOperations;

    public IncomeStatementEntryClassifier(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public boolean isOrdinaryIncomeEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "41")
                && !isSalesReturnEntry(entry)
                && !isOtherIncomeEntry(entry);
    }

    public boolean isSalesReturnEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "417")
                || (codeStartsWith(entry, "41") && nameContainsAllTokens(entry, "devoluciones", "ventas"))
                || nameContains(entry, "devolucion");
    }

    public boolean isOtherIncomeEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "42")
                || nameContainsAllTokens(entry, "otros", "ingresos")
                || nameContains(entry, "recuperacion");
    }

    public boolean isCostOfSalesEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "6")
                || nameContainsAllTokens(entry, "costo", "ventas");
    }

    public boolean isAdministrationExpenseEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "51") && !isDepreciationExpenseEntry(entry);
    }

    public boolean isSalesExpenseEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "52")
                || nameContainsAllTokens(entry, "gastos", "ventas");
    }

    public boolean isFinancialExpenseEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "53")
                || (!codeStartsWith(entry, "4")
                && (nameContains(entry, "financier")
                || nameContains(entry, "interes")));
    }

    public boolean isDepreciationExpenseEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "516")
                || nameContains(entry, "depreci");
    }

    public boolean isIncomeTaxEntry(AccountingEntry entry) {
        return codeStartsWith(entry, "540")
                || nameContainsAllTokens(entry, "impuesto", "renta");
    }

    public boolean isOtherTaxEntry(AccountingEntry entry) {
        return !isIncomeTaxEntry(entry)
                && (codeStartsWith(entry, "54")
                || codeStartsWith(entry, "55")
                || nameContains(entry, "impuesto"));
    }

    private boolean codeStartsWith(AccountingEntry entry, String prefix) {
        return accountingEntryOperations.codeStartsWith(entry, prefix);
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
