package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
class FinancialStatementEquityMatrixBuilder {

    private static final Locale EXPORT_LOCALE = Locale.forLanguageTag("es-CO");

    private final FinancialStatementExportCriteriaResolver financialStatementExportCriteriaResolver;

    List<Integer> resolveYears(
            List<FinancialStatementRow> rows,
            Map<String, Object> financialStatement
    ) {
        Integer criteriaStartYear = financialStatementExportCriteriaResolver.extractYearFromCriteria(financialStatement, "startDate");
        Integer criteriaEndYear = financialStatementExportCriteriaResolver.extractYearFromCriteria(financialStatement, "endDate");

        Set<Integer> years = new LinkedHashSet<>();
        if (criteriaStartYear != null && criteriaEndYear != null) {
            int start = Math.min(criteriaStartYear, criteriaEndYear);
            int end = Math.max(criteriaStartYear, criteriaEndYear);
            for (int year = start; year <= end; year++) {
                years.add(year);
            }
        }

        if (rows != null) {
            for (FinancialStatementRow row : rows) {
                if (row == null || row.getYearValues() == null) {
                    continue;
                }
                for (String key : row.getYearValues().keySet()) {
                    Integer parsedYear = parseYear(key);
                    if (parsedYear != null) {
                        years.add(parsedYear);
                    }
                }
            }
        }

        if (years.isEmpty()) {
            if (criteriaStartYear != null) {
                years.add(criteriaStartYear);
            }
            if (criteriaEndYear != null) {
                years.add(criteriaEndYear);
            }
        }

        if (years.isEmpty()) {
            int currentYear = LocalDate.now().getYear();
            years.add(currentYear - 1);
            years.add(currentYear);
        }

        List<Integer> orderedYears = new ArrayList<>(years);
        orderedYears.sort(Integer::compareTo);
        return orderedYears;
    }

    List<EquityMatrixRow> buildMatrixRows(
            List<FinancialStatementRow> rows,
            List<Integer> years
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<EquityMatrixRow> matrixRows = new ArrayList<>();
        for (FinancialStatementRow row : rows) {
            if (row == null) {
                continue;
            }

            String rowType = row.getRowType();
            if (isSectionRow(rowType)) {
                continue;
            }

            String description = normalizeDescription(row.getLineDescription());
            if (!StringUtils.hasText(description)) {
                continue;
            }

            matrixRows.add(new EquityMatrixRow(
                    description,
                    rowType,
                    resolveYearValues(row, years)
            ));
        }

        matrixRows.sort((left, right) -> {
            int leftOrder = resolveRowOrder(left.description(), left.rowType());
            int rightOrder = resolveRowOrder(right.description(), right.rowType());
            if (leftOrder != rightOrder) {
                return Integer.compare(leftOrder, rightOrder);
            }
            return left.description().compareToIgnoreCase(right.description());
        });

        return matrixRows;
    }

    private Map<Integer, BigDecimal> resolveYearValues(
            FinancialStatementRow row,
            List<Integer> years
    ) {
        Map<Integer, BigDecimal> valuesByYear = new LinkedHashMap<>();
        if (row.getYearValues() != null && !row.getYearValues().isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : row.getYearValues().entrySet()) {
                Integer year = parseYear(entry.getKey());
                BigDecimal value = entry.getValue();
                if (year != null && value != null) {
                    valuesByYear.put(year, value);
                }
            }
        }

        if (valuesByYear.isEmpty() && years != null && !years.isEmpty()) {
            BigDecimal currentAmount = row.getCurrentAmount();
            BigDecimal previousAmount = row.getPreviousAmount();

            if (years.size() == 1) {
                valuesByYear.put(years.get(0), currentAmount != null ? currentAmount : previousAmount);
            } else {
                valuesByYear.put(years.get(0), previousAmount);
                valuesByYear.put(years.get(years.size() - 1), currentAmount);
            }
        }

        Map<Integer, BigDecimal> normalized = new LinkedHashMap<>();
        for (Integer year : years) {
            BigDecimal value = valuesByYear.get(year);
            normalized.put(
                    year,
                    value != null
                            ? value.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }
        return normalized;
    }

    private Integer parseYear(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!text.matches("\\d{4}")) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeDescription(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase(EXPORT_LOCALE);
        if (normalized.contains("total") && normalized.contains("patrimonio")) {
            return "Total patrimonio de los accionistas";
        }
        if (normalized.contains("capital")) {
            return "Capital emitido";
        }
        if ((normalized.contains("ganancia") || normalized.contains("utilidad")) && normalized.contains("ejercicio")) {
            return "Ganancias del ejercicio";
        }
        if (normalized.contains("acumulad") || normalized.contains("retenid") || normalized.contains("resultados")) {
            return "Ganancias acumuladas";
        }
        if (normalized.contains("reserva")) {
            return "Otras reservas";
        }

        return value.trim();
    }

    private int resolveRowOrder(String description, String rowType) {
        String normalized = normalizeDescription(description).toLowerCase(EXPORT_LOCALE);
        if ("TOTAL".equalsIgnoreCase(rowType) || normalized.contains("total patrimonio")) {
            return 50;
        }
        if (normalized.contains("capital")) {
            return 10;
        }
        if (normalized.contains("ejercicio")) {
            return 20;
        }
        if (normalized.contains("acumulad")) {
            return 30;
        }
        if (normalized.contains("reserva")) {
            return 40;
        }
        return 90;
    }

    private boolean isSectionRow(String rowType) {
        return "SECTION".equalsIgnoreCase(rowType)
                || "SUBSECTION".equalsIgnoreCase(rowType);
    }
}
