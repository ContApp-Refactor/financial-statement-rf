package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import java.math.BigDecimal;

public record IncomeStatementSnapshot(
        BigDecimal ordinaryIncome,
        BigDecimal salesReturns,
        BigDecimal netOperatingIncome,
        BigDecimal costOfSales,
        BigDecimal grossProfit,
        BigDecimal otherIncome,
        BigDecimal administrationExpenses,
        BigDecimal salesExpenses,
        BigDecimal financialExpenses,
        BigDecimal depreciationExpenses,
        BigDecimal profitBeforeTaxes,
        BigDecimal incomeTax,
        BigDecimal otherTaxes,
        BigDecimal profitAfterTaxes,
        BigDecimal legalReserve,
        BigDecimal statutoryReserve,
        BigDecimal resultForPeriod
) {
}
