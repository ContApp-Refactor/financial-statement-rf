package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialStatementDataGenerator {

    private final FinancialStatementGenerationStrategyResolver strategyResolver;

    public FinancialStatementDataPayload generate(FinancialStatementRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Financial statement request type is required.");
        }

        return strategyResolver.resolve(request.getType()).generate(request);
    }
}
