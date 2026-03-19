package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import java.util.List;
import java.util.Map;

public record FinancialStatementTableModel(
        String reportName,
        String enterpriseName,
        String generatedAt,
        String criteriaText,
        List<String> columns,
        List<Map<String, Object>> rows,
        List<FinancialStatementSignatureBlock> signatures
) {
}
