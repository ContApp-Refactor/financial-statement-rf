package com.unicauca.edu.co.financial_statements.domain.models.enums;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

public enum EFinancialStatementCriteriaType {
    NUMBER_CLASS(1, "Clase"),
    GROUP(2, "Grupo"),
    ACCOUNT(4, "Cuenta"),
    SUB_ACCOUNT(6, "Subcuenta"),
    AUXILIARY_ACCOUNT(8, "Auxiliar"),
    ACCOUNT_RANGE(6, "Rango de Cuentas");

    private final int prefixLength;
    private final String label;

    EFinancialStatementCriteriaType(int prefixLength, String label) {
        this.prefixLength = prefixLength;
        this.label = label;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<EFinancialStatementCriteriaType> from(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst();
    }

    public static int resolvePrefixLength(String rawValue) {
        return from(rawValue)
                .map(EFinancialStatementCriteriaType::getPrefixLength)
                .orElse(0);
    }

    public static String resolveLabel(String rawValue) {
        return from(rawValue)
                .map(EFinancialStatementCriteriaType::getLabel)
                .orElse(rawValue);
    }
}
