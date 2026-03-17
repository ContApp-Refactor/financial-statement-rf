package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import java.util.List;
import java.util.Map;

record FlattenedFinancialStatementTable(
        List<Map<String, String>> rows,
        List<String> columns
) {
}
