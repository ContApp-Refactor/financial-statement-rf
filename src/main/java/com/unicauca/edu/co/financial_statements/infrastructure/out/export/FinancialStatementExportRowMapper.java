package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAccount;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class FinancialStatementExportRowMapper {

    public List<FinancialStatementRow> toRows(List<Map<String, Object>> rawRows) {
        if (rawRows == null || rawRows.isEmpty()) {
            return List.of();
        }

        return rawRows.stream()
                .map(this::toRow)
                .filter(Objects::nonNull)
                .toList();
    }

    private FinancialStatementRow toRow(Map<String, Object> rawRow) {
        if (rawRow == null || rawRow.isEmpty()) {
            return null;
        }

        return FinancialStatementRow.builder()
                .lineDescription(asText(rawRow.get("lineDescription")))
                .note(asText(rawRow.get("note")))
                .currentAmount(asBigDecimal(rawRow.get("currentAmount")))
                .currentPercentage(asBigDecimal(rawRow.get("currentPercentage")))
                .previousAmount(asBigDecimal(rawRow.get("previousAmount")))
                .previousPercentage(asBigDecimal(rawRow.get("previousPercentage")))
                .variation(asBigDecimal(rawRow.get("variation")))
                .variationPercentage(asBigDecimal(rawRow.get("variationPercentage")))
                .rowType(asText(rawRow.get("rowType")))
                .changeCode(asText(rawRow.get("changeCode")))
                .changeDescription(asText(rawRow.get("changeDescription")))
                .classCode(asText(rawRow.get("classCode")))
                .classDescription(asText(rawRow.get("classDescription")))
                .periodAmount(asBigDecimal(rawRow.get("periodAmount")))
                .nature(asText(rawRow.get("nature")))
                .account(toAccount(rawRow.get("account")))
                .initialBalance(asBigDecimal(rawRow.get("initialBalance")))
                .debitMovement(asBigDecimal(rawRow.get("debitMovement")))
                .creditMovement(asBigDecimal(rawRow.get("creditMovement")))
                .finalBalance(asBigDecimal(rawRow.get("finalBalance")))
                .yearValues(toYearValues(rawRow.get("yearValues")))
                .accountCode(asText(rawRow.get("accountCode")))
                .accountDescription(asText(rawRow.get("accountDescription")))
                .build();
    }

    private FinancialStatementAccount toAccount(Object source) {
        if (!(source instanceof Map<?, ?> map)) {
            return null;
        }

        return FinancialStatementAccount.builder()
                .accountCode(asText(map.get("accountCode")))
                .accountDescription(asText(map.get("accountDescription")))
                .nature(asText(map.get("nature")))
                .build();
    }

    private Map<String, BigDecimal> toYearValues(Object source) {
        if (!(source instanceof Map<?, ?> rawYearValues) || rawYearValues.isEmpty()) {
            return Map.of();
        }

        Map<String, BigDecimal> yearValues = new LinkedHashMap<>();
        rawYearValues.forEach((key, value) -> {
            BigDecimal numericValue = asBigDecimal(value);
            if (key != null && numericValue != null) {
                yearValues.put(String.valueOf(key), numericValue);
            }
        });
        return yearValues;
    }

    private String asText(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
