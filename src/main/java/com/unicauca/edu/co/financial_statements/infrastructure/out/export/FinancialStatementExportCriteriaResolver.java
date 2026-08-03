package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementCriteriaType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class FinancialStatementExportCriteriaResolver {

    private final ObjectMapper objectMapper;

    List<ReportCriterion> resolveCriteria(Map<String, Object> financialStatement) {
        if (financialStatement == null || financialStatement.isEmpty()) {
            return List.of();
        }

        String reportType = asText(financialStatement.get("type"));
        boolean incomeStatement = "INCOME_STATEMENT".equalsIgnoreCase(reportType);
        boolean equityChangesStatement = "STATEMENT_CHANGES_EQUITY".equalsIgnoreCase(reportType);
        boolean financialPositionStatement = "STATEMENT_FINANCIAL_POSITION".equalsIgnoreCase(reportType);

        Map<String, Object> criteria = extractStructuredMap(financialStatement.get("criteria"));
        if (criteria.isEmpty()) {
            return List.of();
        }

        List<ReportCriterion> items = new ArrayList<>();
        String criteriaType = mapCriteriaType(asText(criteria.get("criteriaType")));
        items.add(new ReportCriterion(
                "Tipo de Nivel",
                StringUtils.hasText(criteriaType) ? criteriaType : "Estructura predeterminada"
        ));

        Map<String, Object> range = extractStructuredMap(criteria.get("criteriaRange"));
        if (!range.isEmpty()) {
            String from = asText(range.get("from"));
            String to = asText(range.get("to"));
            if (StringUtils.hasText(from) || StringUtils.hasText(to)) {
                items.add(new ReportCriterion("Rango de Cuentas", "Desde " + from + " hasta " + to));
            }
        }

        String thirdPartyId = asText(criteria.get("thirdPartyId"));
        if (StringUtils.hasText(thirdPartyId)) {
            items.add(new ReportCriterion("Tercero", thirdPartyId));
        }

        String startDate = formatDateForReport(criteria.get("startDate"));
        String endDate = formatDateForReport(criteria.get("endDate"));
        String previousStartDate = formatDateForReport(criteria.get("previousStartDate"));
        String previousEndDate = formatDateForReport(criteria.get("previousEndDate"));
        String previousCutoffDate = formatDateForReport(criteria.get("previousCutoffDate"));
        String currentCutoffDate = formatDateForReport(criteria.get("currentCutoffDate"));

        if (financialPositionStatement) {
            String resolvedPreviousCutoffDate = StringUtils.hasText(previousCutoffDate) ? previousCutoffDate : startDate;
            String resolvedCurrentCutoffDate = StringUtils.hasText(currentCutoffDate) ? currentCutoffDate : endDate;

            if (StringUtils.hasText(resolvedPreviousCutoffDate)) {
                items.add(new ReportCriterion("Fecha de Corte Anterior", resolvedPreviousCutoffDate));
            }

            if (StringUtils.hasText(resolvedCurrentCutoffDate)) {
                items.add(new ReportCriterion("Fecha de Corte Actual", resolvedCurrentCutoffDate));
            }

            return items;
        }

        if (incomeStatement) {
            if (StringUtils.hasText(startDate)) {
                items.add(new ReportCriterion("Fecha de Inicio Periodo Actual", startDate));
            }
            if (StringUtils.hasText(endDate)) {
                items.add(new ReportCriterion("Fecha de Fin Periodo Actual", endDate));
            }
            if (StringUtils.hasText(previousStartDate)) {
                items.add(new ReportCriterion("Fecha de Inicio Periodo Anterior", previousStartDate));
            }
            if (StringUtils.hasText(previousEndDate)) {
                items.add(new ReportCriterion("Fecha de Fin Periodo Anterior", previousEndDate));
            }
            return items;
        }

        if (StringUtils.hasText(startDate)) {
            String label = equityChangesStatement ? "Fecha de Corte Anterior" : "Fecha de Inicio";
            items.add(new ReportCriterion(label, startDate));
        }

        if (StringUtils.hasText(endDate)) {
            String label = equityChangesStatement ? "Fecha de Corte Actual" : "Fecha de Corte";
            items.add(new ReportCriterion(label, endDate));
        }

        return items;
    }

    Integer extractYearFromCriteria(Map<String, Object> financialStatement, String key) {
        if (financialStatement == null || financialStatement.isEmpty() || !StringUtils.hasText(key)) {
            return null;
        }

        Map<String, Object> criteria = extractStructuredMap(financialStatement.get("criteria"));
        if (criteria.isEmpty()) {
            return null;
        }

        Object dateObject = criteria.get(key);
        String dateText = asText(dateObject);
        if (!StringUtils.hasText(dateText)) {
            return null;
        }

        try {
            return LocalDate.parse(dateText).getYear();
        } catch (DateTimeParseException ignored) {
            if (dateText.length() >= 4 && dateText.substring(0, 4).matches("\\d{4}")) {
                try {
                    return Integer.parseInt(dateText.substring(0, 4));
                } catch (NumberFormatException ignoredNumberException) {
                    return null;
                }
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractStructuredMap(Object value) {
        if (value == null) {
            return Map.of();
        }

        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            rawMap.forEach((key, mapValue) -> normalized.put(String.valueOf(key), mapValue));
            return normalized;
        }

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(value);
            Map<String, Object> extracted = new LinkedHashMap<>();
            for (java.beans.PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
                String propertyName = descriptor.getName();
                if ("class".equals(propertyName) || !wrapper.isReadableProperty(propertyName)) {
                    continue;
                }
                extracted.put(propertyName, wrapper.getPropertyValue(propertyName));
            }
            if (!extracted.isEmpty()) {
                return extracted;
            }
        } catch (Exception ignored) {
            // Fall through to ObjectMapper conversion.
        }

        try {
            Map<String, Object> converted = objectMapper.convertValue(value, LinkedHashMap.class);
            return converted != null ? converted : Map.of();
        } catch (IllegalArgumentException exception) {
            return Map.of();
        }
    }

    private String mapCriteriaType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return null;
        }
        return EFinancialStatementCriteriaType.resolveLabel(rawType);
    }

    private String formatDateForReport(Object value) {
        String raw = asText(value);
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        try {
            return LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ignored) {
            return raw;
        }
    }

    private String asText(Object value) {
        return value != null ? String.valueOf(value).trim() : null;
    }
}
