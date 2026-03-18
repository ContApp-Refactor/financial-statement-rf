package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FinancialStatementGenerationStrategyResolver {

    private final Map<EFinancialStatementType, FinancialStatementGenerationStrategy> strategiesByType;

    public FinancialStatementGenerationStrategyResolver(List<FinancialStatementGenerationStrategy> strategies) {
        this.strategiesByType = new EnumMap<>(EFinancialStatementType.class);

        if (strategies == null || strategies.isEmpty()) {
            return;
        }

        for (FinancialStatementGenerationStrategy strategy : strategies) {
            if (strategy == null || strategy.supports() == null) {
                continue;
            }

            FinancialStatementGenerationStrategy existing = strategiesByType.putIfAbsent(strategy.supports(), strategy);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate financial statement generation strategy for type " + strategy.supports() + "."
                );
            }
        }
    }

    public FinancialStatementGenerationStrategy resolve(EFinancialStatementType type) {
        if (type == null) {
            throw new IllegalArgumentException("Financial statement type is required.");
        }

        FinancialStatementGenerationStrategy strategy = strategiesByType.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No financial statement generation strategy configured for type " + type + ".");
        }

        return strategy;
    }
}
