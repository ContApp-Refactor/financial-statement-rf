package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
class FinancialStatementTabularDataResolver {

    private final ObjectMapper objectMapper;

    FlattenedFinancialStatementTable resolve(
            List<Map<String, Object>> rows,
            List<FinancialStatementRow> typedRows
    ) {
        List<Map<String, String>> flattenedRows = typedRows != null && !typedRows.isEmpty()
                ? flattenTypedRows(typedRows)
                : flattenRows(rows);

        return new FlattenedFinancialStatementTable(flattenedRows, resolveColumns(flattenedRows));
    }

    private List<Map<String, String>> flattenRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<Map<String, String>> flattenedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, String> flattened = new LinkedHashMap<>();
            flattenMap("", row, flattened);
            flattenedRows.add(flattened);
        }
        return flattenedRows;
    }

    private List<Map<String, String>> flattenTypedRows(List<FinancialStatementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<Map<String, String>> flattenedRows = new ArrayList<>();
        for (FinancialStatementRow row : rows) {
            if (row == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawRow = objectMapper.convertValue(row, LinkedHashMap.class);
            Map<String, String> flattened = new LinkedHashMap<>();
            flattenMap("", rawRow, flattened);
            flattenedRows.add(flattened);
        }
        return flattenedRows;
    }

    private void flattenMap(String prefix, Map<String, Object> source, Map<String, String> target) {
        if (source == null || source.isEmpty()) {
            return;
        }

        source.forEach((key, value) -> {
            String composedKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> nestedMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) nestedMap;
                flattenMap(composedKey, casted, target);
            } else {
                target.put(composedKey, value != null ? String.valueOf(value) : "");
            }
        });
    }

    private List<String> resolveColumns(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Set<String> orderedColumns = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            orderedColumns.addAll(row.keySet());
        }
        return List.copyOf(orderedColumns);
    }
}
