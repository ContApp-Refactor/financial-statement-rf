package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
class FinancialStatementPositionTableBuilder {

    private static final String TOTAL_ACTIVO_CORRIENTE_LABEL = "TOTAL ACTIVO CORRIENTE";
    private static final String TOTAL_ACTIVO_NO_CORRIENTE_LABEL = "TOTAL ACTIVO NO CORRIENTE";
    private static final String TOTAL_ACTIVO_LABEL = "TOTAL ACTIVO";
    private static final String TOTAL_PASIVO_CORRIENTE_LABEL = "TOTAL PASIVO CORRIENTE";
    private static final String TOTAL_PASIVO_NO_CORRIENTE_LABEL = "TOTAL PASIVO NO CORRIENTE";
    private static final String TOTAL_PASIVO_LABEL = "TOTAL PASIVO";
    private static final String TOTAL_PATRIMONIO_LABEL = "TOTAL PATRIMONIO";
    private static final String TOTAL_PASIVO_MAS_PATRIMONIO_LABEL = "TOTAL PASIVO + PATRIMONIO";
    private static final String TOTAL_INGRESOS_LABEL = "TOTAL INGRESOS";
    private static final String TOTAL_COSTOS_VENTAS_LABEL = "TOTAL COSTOS DE VENTAS";
    private static final String TOTAL_GASTOS_OPERACIONALES_LABEL = "TOTAL GASTOS OPERACIONALES";
    private static final String UTILIDAD_BRUTA_LABEL = "UTILIDAD BRUTA";
    private static final String UTILIDAD_OPERACIONAL_LABEL = "UTILIDAD OPERACIONAL";
    private static final String UTILIDAD_NETA_LABEL = "UTILIDAD NETA DEL EJERCICIO";
    private static final Locale EXPORT_LOCALE = Locale.forLanguageTag("es-CO");

    List<FinancialPositionRow> toRows(List<FinancialStatementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<FinancialPositionRow> mappedRows = new ArrayList<>();
        for (FinancialStatementRow row : rows) {
            if (row == null) {
                continue;
            }

            mappedRows.add(new FinancialPositionRow(
                    row.getLineDescription(),
                    row.getNote(),
                    row.getCurrentAmount(),
                    row.getCurrentPercentage(),
                    row.getPreviousAmount(),
                    row.getPreviousPercentage(),
                    row.getVariation(),
                    row.getVariationPercentage(),
                    row.getRowType()
            ));
        }
        return mappedRows;
    }

    boolean supports(List<FinancialStatementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }

        return rows.stream()
                .filter(Objects::nonNull)
                .anyMatch(row -> StringUtils.hasText(row.getLineDescription())
                        && StringUtils.hasText(row.getRowType())
                        && (row.getCurrentAmount() != null || row.getPreviousAmount() != null));
    }

    boolean isSectionRow(String rowType) {
        return "SECTION".equalsIgnoreCase(rowType)
                || "SUBSECTION".equalsIgnoreCase(rowType);
    }

    boolean isTotalRow(String rowType) {
        return "TOTAL".equalsIgnoreCase(rowType);
    }

    String resolveTotalAmountFormula(
            String totalLabel,
            char columnLetter,
            int totalRowNumber,
            Integer subsectionStartRowNumber,
            Map<String, Integer> totalRowNumberByLabel
    ) {
        String normalizedTotalLabel = normalizeLabel(totalLabel);
        if (!StringUtils.hasText(normalizedTotalLabel)) {
            return null;
        }

        if (TOTAL_ACTIVO_LABEL.equals(normalizedTotalLabel)) {
            return directAdditionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(TOTAL_ACTIVO_CORRIENTE_LABEL),
                    totalRowNumberByLabel.get(TOTAL_ACTIVO_NO_CORRIENTE_LABEL)
            );
        }
        if (TOTAL_PASIVO_LABEL.equals(normalizedTotalLabel)) {
            return directAdditionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(TOTAL_PASIVO_CORRIENTE_LABEL),
                    totalRowNumberByLabel.get(TOTAL_PASIVO_NO_CORRIENTE_LABEL)
            );
        }
        if (TOTAL_PATRIMONIO_LABEL.equals(normalizedTotalLabel)) {
            return directSubtractionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(TOTAL_ACTIVO_LABEL),
                    totalRowNumberByLabel.get(TOTAL_PASIVO_LABEL)
            );
        }
        if (TOTAL_PASIVO_MAS_PATRIMONIO_LABEL.equals(normalizedTotalLabel)) {
            return directAdditionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(TOTAL_PASIVO_LABEL),
                    totalRowNumberByLabel.get(TOTAL_PATRIMONIO_LABEL)
            );
        }
        if (UTILIDAD_BRUTA_LABEL.equals(normalizedTotalLabel)) {
            return directSubtractionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(TOTAL_INGRESOS_LABEL),
                    totalRowNumberByLabel.get(TOTAL_COSTOS_VENTAS_LABEL)
            );
        }
        if (UTILIDAD_OPERACIONAL_LABEL.equals(normalizedTotalLabel)) {
            return directSubtractionFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(UTILIDAD_BRUTA_LABEL),
                    totalRowNumberByLabel.get(TOTAL_GASTOS_OPERACIONALES_LABEL)
            );
        }
        if (UTILIDAD_NETA_LABEL.equals(normalizedTotalLabel)) {
            return directReferenceFormula(
                    columnLetter,
                    totalRowNumberByLabel.get(UTILIDAD_OPERACIONAL_LABEL)
            );
        }

        if (subsectionStartRowNumber == null || !normalizedTotalLabel.startsWith("TOTAL ")) {
            return null;
        }

        return sumFormula(columnLetter, subsectionStartRowNumber + 1, totalRowNumber - 1);
    }

    String normalizeLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(EXPORT_LOCALE);
    }

    private String directAdditionFormula(char columnLetter, Integer firstRow, Integer secondRow) {
        if (firstRow == null || secondRow == null) {
            return null;
        }
        return String.valueOf(columnLetter) + firstRow + "+" + columnLetter + secondRow;
    }

    private String directSubtractionFormula(char columnLetter, Integer minuendRow, Integer subtrahendRow) {
        if (minuendRow == null || subtrahendRow == null) {
            return null;
        }
        return String.valueOf(columnLetter) + minuendRow + "-" + columnLetter + subtrahendRow;
    }

    private String directReferenceFormula(char columnLetter, Integer referencedRow) {
        if (referencedRow == null) {
            return null;
        }
        return String.valueOf(columnLetter) + referencedRow;
    }

    private String sumFormula(char columnLetter, int startRow, int endRow) {
        if (startRow > endRow) {
            return "0";
        }
        return "SUM(" + columnLetter + startRow + ":" + columnLetter + endRow + ")";
    }
}
