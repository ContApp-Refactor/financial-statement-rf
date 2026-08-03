package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeStatementEntryClassifierTest {

    private final IncomeStatementEntryClassifier classifier =
            new IncomeStatementEntryClassifier(new AccountingEntryOperations());

    @Test
    void shouldClassifyOrdinaryIncomeWithoutMixingReturnsOrOtherIncome() {
        AccountingEntry ordinaryIncome = entry("413505", "Ingresos operacionales - Ventas nacionales");
        AccountingEntry salesReturn = entry("417505", "Ingresos operacionales - Devoluciones en ventas");
        AccountingEntry otherIncome = entry("421505", "Otros ingresos - Recuperaciones");

        assertThat(classifier.isOrdinaryIncomeEntry(ordinaryIncome)).isTrue();
        assertThat(classifier.isOrdinaryIncomeEntry(salesReturn)).isFalse();
        assertThat(classifier.isOrdinaryIncomeEntry(otherIncome)).isFalse();
    }

    @Test
    void shouldClassifyAdministrationExpenseWithoutMixingDepreciation() {
        AccountingEntry administrationExpense = entry("510505", "Gastos operacionales de administracion - Sueldos");
        AccountingEntry depreciationExpense = entry("516005", "Gastos de administracion - Depreciacion");

        assertThat(classifier.isAdministrationExpenseEntry(administrationExpense)).isTrue();
        assertThat(classifier.isAdministrationExpenseEntry(depreciationExpense)).isFalse();
        assertThat(classifier.isDepreciationExpenseEntry(depreciationExpense)).isTrue();
    }

    @Test
    void shouldClassifyFinancialExpenseWithoutMixingInterestIncome() {
        AccountingEntry financialExpense = entry("530505", "Gastos financieros - Intereses");
        AccountingEntry interestIncome = entry("421005", "Otros ingresos - Intereses");

        assertThat(classifier.isFinancialExpenseEntry(financialExpense)).isTrue();
        assertThat(classifier.isFinancialExpenseEntry(interestIncome)).isFalse();
    }

    @Test
    void shouldClassifyTaxesSeparatingIncomeTaxFromOtherTaxes() {
        AccountingEntry incomeTax = entry("540505", "Impuesto de renta");
        AccountingEntry otherTax = entry("541005", "Otros impuestos");

        assertThat(classifier.isIncomeTaxEntry(incomeTax)).isTrue();
        assertThat(classifier.isOtherTaxEntry(incomeTax)).isFalse();
        assertThat(classifier.isOtherTaxEntry(otherTax)).isTrue();
    }

    @Test
    void shouldClassifyCostOfSalesAndSalesExpensesByCodeAndName() {
        AccountingEntry costOfSales = entry("613505", "Costo de ventas - Mercancia no fabricada por la empresa");
        AccountingEntry salesExpense = entry("520505", "Gastos operacionales de ventas - Comisiones");

        assertThat(classifier.isCostOfSalesEntry(costOfSales)).isTrue();
        assertThat(classifier.isSalesExpenseEntry(salesExpense)).isTrue();
    }

    @Test
    void shouldPreferAccountCodeWhenTheNameLooksLikeAnotherCategory() {
        AccountingEntry ordinaryIncomeWithConfusingName = entry("413505", "Otros ingresos - Recuperaciones");
        AccountingEntry otherIncomeWithConfusingName = entry("421505", "Ventas nacionales");
        AccountingEntry financialExpenseWithIncomeName = entry("530505", "Ingreso por intereses");
        AccountingEntry otherIncomeWithExpenseName = entry("421005", "Gastos financieros");

        assertThat(classifier.isOrdinaryIncomeEntry(ordinaryIncomeWithConfusingName)).isTrue();
        assertThat(classifier.isOtherIncomeEntry(ordinaryIncomeWithConfusingName)).isFalse();

        assertThat(classifier.isOtherIncomeEntry(otherIncomeWithConfusingName)).isTrue();
        assertThat(classifier.isOrdinaryIncomeEntry(otherIncomeWithConfusingName)).isFalse();

        assertThat(classifier.isFinancialExpenseEntry(financialExpenseWithIncomeName)).isTrue();
        assertThat(classifier.isFinancialExpenseEntry(otherIncomeWithExpenseName)).isFalse();
        assertThat(classifier.isOtherIncomeEntry(otherIncomeWithExpenseName)).isTrue();
    }

    private AccountingEntry entry(String code, String name) {
        return AccountingEntry.builder()
                .accountCode(code)
                .accountName(name)
                .build();
    }
}
