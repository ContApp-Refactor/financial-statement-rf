package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
class DefaultFinancialStatementTableModelResolver implements FinancialStatementTableModelResolver {

    private static final Locale EXPORT_LOCALE = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FinancialStatementExportCriteriaResolver criteriaResolver;
    private final FinancialStatementEquityMatrixBuilder equityMatrixBuilder;
    private final FinancialStatementPositionTableBuilder positionTableBuilder;
    private final FinancialStatementTabularDataResolver tabularDataResolver;

    @Override
    public FinancialStatementTableModel resolve(
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rawRows,
            List<FinancialStatementRow> typedRows,
            Map<String, Object> financialStatement,
            List<String> annotationTexts,
            byte[] signatureImage
    ) {
        List<String> columns;
        List<Map<String, Object>> rows;

        if (isEquityChangesStatement(financialStatement)) {
            List<Integer> years = equityMatrixBuilder.resolveYears(typedRows, financialStatement);
            List<EquityMatrixRow> matrixRows = equityMatrixBuilder.buildMatrixRows(typedRows, years);
            columns = new ArrayList<>();
            columns.add("Descripcion");
            for (Integer year : years) {
                columns.add(String.valueOf(year));
            }
            rows = toEquityRows(matrixRows, years);
        } else if (positionTableBuilder.supports(typedRows)) {
            columns = List.of(
                    "Descripcion",
                    "Valor Actual",
                    "% Actual",
                    "Valor Anterior",
                    "% Anterior",
                    "Variacion",
                    "% Variacion"
            );
            rows = toFinancialPositionRows(positionTableBuilder.toRows(typedRows));
        } else {
            FlattenedFinancialStatementTable table = tabularDataResolver.resolve(rawRows, typedRows);
            columns = table.columns();
            rows = toGenericRows(table);
        }

        if (columns == null || columns.isEmpty()) {
            columns = List.of("Descripcion");
        }
        if (rows.isEmpty()) {
            rows = List.of(singleMessageRow(columns.size(), "No data available"));
        }

        appendAnnotations(rows, columns.size(), annotationTexts);

        return new FinancialStatementTableModel(
                reportName,
                enterpriseName,
                LocalDateTime.now().format(GENERATED_AT_FORMAT),
                buildCriteriaText(financialStatement),
                columns,
                rows,
                signatureImage
        );
    }

    private List<Map<String, Object>> toFinancialPositionRows(List<FinancialPositionRow> financialRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinancialPositionRow financialRow : financialRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("column1", financialRow.lineDescription());
            row.put("column2", formatAmount(financialRow.currentAmount()));
            row.put("column3", formatPercentage(financialRow.currentPercentage()));
            row.put("column4", formatAmount(financialRow.previousAmount()));
            row.put("column5", formatPercentage(financialRow.previousPercentage()));
            row.put("column6", formatAmount(financialRow.variation()));
            row.put("column7", formatPercentage(financialRow.variationPercentage()));
            row.put("rowType", financialRow.rowType());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toEquityRows(
            List<EquityMatrixRow> matrixRows,
            List<Integer> years
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EquityMatrixRow matrixRow : matrixRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("column1", matrixRow.description());
            for (int index = 0; index < years.size(); index++) {
                Integer year = years.get(index);
                row.put("column" + (index + 2), formatAmount(matrixRow.valuesByYear().get(year)));
            }
            row.put("rowType", matrixRow.rowType());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toGenericRows(FlattenedFinancialStatementTable table) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, String> sourceRow : table.rows()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < table.columns().size(); index++) {
                String column = table.columns().get(index);
                row.put("column" + (index + 1), sourceRow.getOrDefault(column, ""));
            }
            row.put("rowType", asText(sourceRow.get("rowType")));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> singleMessageRow(int columnCount, String message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("column1", message);
        for (int index = 2; index <= columnCount; index++) {
            row.put("column" + index, "");
        }
        row.put("rowType", "DETAIL");
        return row;
    }

    private String buildCriteriaText(Map<String, Object> financialStatement) {
        List<ReportCriterion> criteria = criteriaResolver.resolveCriteria(financialStatement);
        if (criteria.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("Criterios Utilizados:");
        for (ReportCriterion criterion : criteria) {
            builder.append(System.lineSeparator())
                    .append(criterion.key())
                    .append(": ")
                    .append(criterion.value());
        }
        return builder.toString();
    }

    private boolean isEquityChangesStatement(Map<String, Object> financialStatement) {
        if (financialStatement == null || financialStatement.isEmpty()) {
            return false;
        }
        return "STATEMENT_CHANGES_EQUITY".equalsIgnoreCase(asText(financialStatement.get("type")));
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return createDecimalFormatter().format(value);
    }

    private String formatPercentage(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return createDecimalFormatter().format(value) + "%";
    }

    private DecimalFormat createDecimalFormatter() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(EXPORT_LOCALE);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", symbols);
    }

    private String asText(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : "";
    }

    private void appendAnnotations(List<Map<String, Object>> rows, int columnCount, List<String> annotationTexts) {
        if (rows == null || annotationTexts == null || annotationTexts.isEmpty()) {
            return;
        }

        rows.add(singleMessageRow(columnCount, ""));
        rows.add(singleMessageRow(columnCount, "ANOTACIONES"));

        int annotationIndex = 1;
        for (String annotationText : annotationTexts) {
            if (!StringUtils.hasText(annotationText)) {
                continue;
            }
            rows.add(singleMessageRow(columnCount, annotationIndex + ". " + annotationText.trim()));
            annotationIndex++;
        }
    }
}
