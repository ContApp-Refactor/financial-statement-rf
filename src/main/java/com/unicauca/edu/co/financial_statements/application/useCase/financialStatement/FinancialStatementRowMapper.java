package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAccount;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class FinancialStatementRowMapper {

    public List<FinancialStatementRow> toTypedRows(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(this::toTypedRow)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Map<String, Object>> toRowMaps(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(this::toRowMap)
                .filter(Objects::nonNull)
                .toList();
    }

    public FinancialStatementRow toTypedRow(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof FinancialStatementRow row) {
            return row;
        }
        if (!(source instanceof Map<?, ?> map)) {
            return null;
        }

        return FinancialStatementRow.builder()
                .lineDescription(asString(map.get("lineDescription")))
                .note(asString(map.get("note")))
                .currentAmount(asBigDecimal(map.get("currentAmount")))
                .currentPercentage(asBigDecimal(map.get("currentPercentage")))
                .previousAmount(asBigDecimal(map.get("previousAmount")))
                .previousPercentage(asBigDecimal(map.get("previousPercentage")))
                .variation(asBigDecimal(map.get("variation")))
                .variationPercentage(asBigDecimal(map.get("variationPercentage")))
                .rowType(asString(map.get("rowType")))
                .changeCode(asString(map.get("changeCode")))
                .changeDescription(asString(map.get("changeDescription")))
                .classCode(asString(map.get("classCode")))
                .classDescription(asString(map.get("classDescription")))
                .periodAmount(asBigDecimal(map.get("periodAmount")))
                .nature(asString(map.get("nature")))
                .account(toAccount(map.get("account")))
                .initialBalance(asBigDecimal(map.get("initialBalance")))
                .debitMovement(asBigDecimal(map.get("debitMovement")))
                .creditMovement(asBigDecimal(map.get("creditMovement")))
                .finalBalance(asBigDecimal(map.get("finalBalance")))
                .yearValues(toYearValues(map.get("yearValues")))
                .accountCode(asString(map.get("accountCode")))
                .accountDescription(asString(map.get("accountDescription")))
                .build();
    }

    public Map<String, Object> toRowMap(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(LinkedHashMap::new, (acc, entry) -> acc.put(String.valueOf(entry.getKey()), entry.getValue()), Map::putAll);
        }
        if (!(source instanceof FinancialStatementRow row)) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lineDescription", row.getLineDescription());
        map.put("note", row.getNote());
        map.put("currentAmount", row.getCurrentAmount());
        map.put("currentPercentage", row.getCurrentPercentage());
        map.put("previousAmount", row.getPreviousAmount());
        map.put("previousPercentage", row.getPreviousPercentage());
        map.put("variation", row.getVariation());
        map.put("variationPercentage", row.getVariationPercentage());
        map.put("rowType", row.getRowType());

        putIfNotNull(map, "changeCode", row.getChangeCode());
        putIfNotNull(map, "changeDescription", row.getChangeDescription());
        putIfNotNull(map, "classCode", row.getClassCode());
        putIfNotNull(map, "classDescription", row.getClassDescription());
        putIfNotNull(map, "periodAmount", row.getPeriodAmount());
        putIfNotNull(map, "nature", row.getNature());
        putIfNotNull(map, "initialBalance", row.getInitialBalance());
        putIfNotNull(map, "debitMovement", row.getDebitMovement());
        putIfNotNull(map, "creditMovement", row.getCreditMovement());
        putIfNotNull(map, "finalBalance", row.getFinalBalance());
        if (row.getYearValues() != null && !row.getYearValues().isEmpty()) {
            map.put("yearValues", row.getYearValues());
        }
        if (row.getAccount() != null) {
            Map<String, Object> account = new LinkedHashMap<>();
            account.put("accountCode", row.getAccount().getAccountCode());
            account.put("accountDescription", row.getAccount().getAccountDescription());
            account.put("nature", row.getAccount().getNature());
            map.put("account", account);
        }
        putIfNotNull(map, "accountCode", row.getAccountCode());
        putIfNotNull(map, "accountDescription", row.getAccountDescription());
        return map;
    }

    private FinancialStatementAccount toAccount(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof FinancialStatementAccount account) {
            return account;
        }
        if (!(source instanceof Map<?, ?> map)) {
            return null;
        }

        return FinancialStatementAccount.builder()
                .accountCode(asString(map.get("accountCode")))
                .accountDescription(asString(map.get("accountDescription")))
                .nature(asString(map.get("nature")))
                .build();
    }

    private Map<String, BigDecimal> toYearValues(Object source) {
        if (!(source instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }

        Map<String, BigDecimal> yearValues = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            BigDecimal numericValue = asBigDecimal(value);
            if (key != null && numericValue != null) {
                yearValues.put(String.valueOf(key), numericValue);
            }
        });
        return yearValues;
    }

    private String asString(Object value) {
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

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
