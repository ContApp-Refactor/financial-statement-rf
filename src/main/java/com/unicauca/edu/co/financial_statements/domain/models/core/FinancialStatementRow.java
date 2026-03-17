package com.unicauca.edu.co.financial_statements.domain.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementRow {
    private String lineDescription;
    private String note;
    private BigDecimal currentAmount;
    private BigDecimal currentPercentage;
    private BigDecimal previousAmount;
    private BigDecimal previousPercentage;
    private BigDecimal variation;
    private BigDecimal variationPercentage;
    private String rowType;
    private String changeCode;
    private String changeDescription;
    private String classCode;
    private String classDescription;
    private BigDecimal periodAmount;
    private String nature;
    private FinancialStatementAccount account;
    private BigDecimal initialBalance;
    private BigDecimal debitMovement;
    private BigDecimal creditMovement;
    private BigDecimal finalBalance;
    private Map<String, BigDecimal> yearValues;
    private String accountCode;
    private String accountDescription;
}
