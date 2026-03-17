package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;

import java.util.List;
import java.util.Map;

public interface FinancialStatementTableModelResolver {

    FinancialStatementTableModel resolve(
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rawRows,
            List<FinancialStatementRow> typedRows,
            Map<String, Object> financialStatement
    );
}
