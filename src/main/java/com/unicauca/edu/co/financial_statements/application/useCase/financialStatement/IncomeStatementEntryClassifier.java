package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;

@Component
public class IncomeStatementEntryClassifier {

    private final AccountingEntryOperations accountingEntryOperations;

    public IncomeStatementEntryClassifier(AccountingEntryOperations accountingEntryOperations) {
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public boolean isOrdinaryIncomeEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "4")
                && accountingEntryOperations.matchesGroup(entry, "41")
                && !isSalesReturnEntry(entry);
    }

    public boolean isSalesReturnEntry(AccountingEntry entry) {
        return accountingEntryOperations.codeStartsWith(entry, "417");
    }

    public boolean isOtherIncomeEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesGroup(entry, "42");
    }

    public boolean isCostOfSalesEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "6");
    }

    public boolean isAdministrationExpenseEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesGroup(entry, "51") && !isDepreciationExpenseEntry(entry);
    }

    public boolean isSalesExpenseEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesGroup(entry, "52");
    }

    public boolean isFinancialExpenseEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesGroup(entry, "53");
    }

    public boolean isDepreciationExpenseEntry(AccountingEntry entry) {
        return accountingEntryOperations.codeStartsWith(entry, "516");
    }

    public boolean isIncomeTaxEntry(AccountingEntry entry) {
        return accountingEntryOperations.codeStartsWith(entry, "540");
    }

    public boolean isOtherTaxEntry(AccountingEntry entry) {
        return accountingEntryOperations.matchesAccountingClass(entry, "5")
                && (accountingEntryOperations.matchesGroup(entry, "54")
                || accountingEntryOperations.matchesGroup(entry, "55"))
                && !isIncomeTaxEntry(entry);
    }
}
