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
            throw new IllegalArgumentException("El tipo de reporte de la solicitud es obligatorio.");
        }

        return strategyResolver.resolve(request.getType()).generate(request);
    }
}
