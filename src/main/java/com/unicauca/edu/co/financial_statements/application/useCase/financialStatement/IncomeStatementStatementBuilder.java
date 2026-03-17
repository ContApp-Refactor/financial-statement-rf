package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class IncomeStatementStatementBuilder {

    private final IncomeStatementAmountCalculator incomeStatementAmountCalculator;
    private final IncomeStatementEntryClassifier incomeStatementEntryClassifier;
    private final FinancialStatementComparativeRowBuilder financialStatementComparativeRowBuilder;

    public IncomeStatementStatementBuilder(
            IncomeStatementAmountCalculator incomeStatementAmountCalculator,
            IncomeStatementEntryClassifier incomeStatementEntryClassifier,
            FinancialStatementComparativeRowBuilder financialStatementComparativeRowBuilder
    ) {
        this.incomeStatementAmountCalculator = incomeStatementAmountCalculator;
        this.incomeStatementEntryClassifier = incomeStatementEntryClassifier;
        this.financialStatementComparativeRowBuilder = financialStatementComparativeRowBuilder;
    }

    public List<Map<String, Object>> buildRows(
            List<AccountingEntry> accountingEntries,
            List<AccountingEntry> previousAccountingEntries,
            FinancialStatementCriteria criteria
    ) {
        List<AccountingEntry> currentEntries = accountingEntries != null ? accountingEntries : List.of();
        List<AccountingEntry> previousEntries = previousAccountingEntries != null ? previousAccountingEntries : List.of();

        IncomeStatementSnapshot currentSnapshot = buildSnapshot(currentEntries);
        IncomeStatementSnapshot previousSnapshot = buildSnapshot(previousEntries);

        BigDecimal currentPercentageBase = resolvePercentageBase(currentSnapshot);
        BigDecimal previousPercentageBase = resolvePercentageBase(previousSnapshot);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildRow("Ingresos ordinarios", currentSnapshot.ordinaryIncome(), previousSnapshot.ordinaryIncome(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isOrdinaryIncomeEntry,
                currentPercentageBase,
                previousPercentageBase,
                false
        ));
        rows.add(buildRow("(-) Devoluciones en ventas", currentSnapshot.salesReturns(), previousSnapshot.salesReturns(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isSalesReturnEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("INGRESOS NETOS OPERACIONALES", currentSnapshot.netOperatingIncome(), previousSnapshot.netOperatingIncome(), "TOTAL", currentPercentageBase, previousPercentageBase));
        rows.add(buildRow("(-) Costo de ventas", currentSnapshot.costOfSales(), previousSnapshot.costOfSales(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isCostOfSalesEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("UTILIDAD BRUTA ORDINARIA", currentSnapshot.grossProfit(), previousSnapshot.grossProfit(), "TOTAL", currentPercentageBase, previousPercentageBase));
        rows.add(buildRow("(+) Otros ingresos", currentSnapshot.otherIncome(), previousSnapshot.otherIncome(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isOtherIncomeEntry,
                currentPercentageBase,
                previousPercentageBase,
                false
        ));
        rows.add(buildRow("(-) Gastos de administracion", currentSnapshot.administrationExpenses(), previousSnapshot.administrationExpenses(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isAdministrationExpenseEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("(-) Gastos de ventas", currentSnapshot.salesExpenses(), previousSnapshot.salesExpenses(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isSalesExpenseEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("(-) Gastos financieros", currentSnapshot.financialExpenses(), previousSnapshot.financialExpenses(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isFinancialExpenseEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("(-) Gastos depreciacion", currentSnapshot.depreciationExpenses(), previousSnapshot.depreciationExpenses(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isDepreciationExpenseEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("UTILIDAD ANTES DE IMPUESTOS", currentSnapshot.profitBeforeTaxes(), previousSnapshot.profitBeforeTaxes(), "TOTAL", currentPercentageBase, previousPercentageBase));
        rows.add(buildRow("(-) Impuesto de renta", currentSnapshot.incomeTax(), previousSnapshot.incomeTax(), "DETAIL", currentPercentageBase, previousPercentageBase));
        rows.addAll(buildLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                incomeStatementEntryClassifier::isIncomeTaxEntry,
                currentPercentageBase,
                previousPercentageBase,
                true
        ));
        rows.add(buildRow("UTILIDAD DESPUES DE IMPUESTOS", currentSnapshot.profitAfterTaxes(), previousSnapshot.profitAfterTaxes(), "TOTAL", currentPercentageBase, previousPercentageBase));
        rows.add(buildRow("RESULTADO DEL EJERCICIO", currentSnapshot.resultForPeriod(), previousSnapshot.resultForPeriod(), "TOTAL", currentPercentageBase, previousPercentageBase));
        return rows;
    }

    private IncomeStatementSnapshot buildSnapshot(List<AccountingEntry> entries) {
        List<AccountingEntry> safeEntries = entries != null ? entries : List.of();

        BigDecimal ordinaryIncome = sumEntries(safeEntries, incomeStatementEntryClassifier::isOrdinaryIncomeEntry, false);
        BigDecimal salesReturns = sumEntries(safeEntries, incomeStatementEntryClassifier::isSalesReturnEntry, true);
        BigDecimal netOperatingIncome = scaleAmount(ordinaryIncome.subtract(salesReturns));

        BigDecimal costOfSales = sumEntries(safeEntries, incomeStatementEntryClassifier::isCostOfSalesEntry, true);
        BigDecimal grossProfit = scaleAmount(netOperatingIncome.subtract(costOfSales));

        BigDecimal otherIncome = sumEntries(safeEntries, incomeStatementEntryClassifier::isOtherIncomeEntry, false);
        BigDecimal administrationExpenses = sumEntries(safeEntries, incomeStatementEntryClassifier::isAdministrationExpenseEntry, true);
        BigDecimal salesExpenses = sumEntries(safeEntries, incomeStatementEntryClassifier::isSalesExpenseEntry, true);
        BigDecimal financialExpenses = sumEntries(safeEntries, incomeStatementEntryClassifier::isFinancialExpenseEntry, true);
        BigDecimal depreciationExpenses = sumEntries(safeEntries, incomeStatementEntryClassifier::isDepreciationExpenseEntry, true);

        BigDecimal profitBeforeTaxes = scaleAmount(
                grossProfit
                        .add(otherIncome)
                        .subtract(administrationExpenses)
                        .subtract(salesExpenses)
                        .subtract(financialExpenses)
                        .subtract(depreciationExpenses)
        );

        BigDecimal incomeTax = sumEntries(safeEntries, incomeStatementEntryClassifier::isIncomeTaxEntry, true);
        BigDecimal otherTaxes = sumEntries(safeEntries, incomeStatementEntryClassifier::isOtherTaxEntry, true);
        BigDecimal profitAfterTaxes = scaleAmount(profitBeforeTaxes.subtract(incomeTax).subtract(otherTaxes));

        BigDecimal legalReserve = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal statutoryReserve = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal resultForPeriod = scaleAmount(profitAfterTaxes);

        return new IncomeStatementSnapshot(
                ordinaryIncome,
                salesReturns,
                netOperatingIncome,
                costOfSales,
                grossProfit,
                otherIncome,
                administrationExpenses,
                salesExpenses,
                financialExpenses,
                depreciationExpenses,
                profitBeforeTaxes,
                incomeTax,
                otherTaxes,
                profitAfterTaxes,
                legalReserve,
                statutoryReserve,
                resultForPeriod
        );
    }

    private BigDecimal resolvePercentageBase(IncomeStatementSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (snapshot.ordinaryIncome().compareTo(BigDecimal.ZERO) != 0) {
            return snapshot.ordinaryIncome();
        }
        if (snapshot.netOperatingIncome().compareTo(BigDecimal.ZERO) != 0) {
            return snapshot.netOperatingIncome();
        }
        return null;
    }

    private BigDecimal sumEntries(
            List<AccountingEntry> entries,
            java.util.function.Predicate<AccountingEntry> matcher,
            boolean absolute
    ) {
        return incomeStatementAmountCalculator.sumEntries(entries, matcher, absolute);
    }

    private List<Map<String, Object>> buildLevelRows(
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            FinancialStatementCriteria criteria,
            java.util.function.Predicate<AccountingEntry> matcher,
            BigDecimal currentPercentageBase,
            BigDecimal previousPercentageBase,
            boolean absoluteAmounts
    ) {
        return financialStatementComparativeRowBuilder.buildLevelComparisonRows(
                currentEntries,
                previousEntries,
                criteria,
                matcher,
                currentPercentageBase,
                previousPercentageBase,
                incomeStatementAmountCalculator::signedAmount,
                absoluteAmounts
        );
    }

    private Map<String, Object> buildRow(
            String lineDescription,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            String rowType,
            BigDecimal currentPercentageBase,
            BigDecimal previousPercentageBase
    ) {
        return financialStatementComparativeRowBuilder.buildComparativeRow(
                lineDescription,
                null,
                currentAmount,
                previousAmount,
                rowType,
                currentPercentageBase,
                previousPercentageBase
        );
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
