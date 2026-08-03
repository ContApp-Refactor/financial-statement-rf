package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialStatementGenerationStrategyResolverTest {

    @Test
    void shouldResolveStrategyBySupportedType() {
        FinancialStatementGenerationStrategy incomeStrategy = strategy(EFinancialStatementType.INCOME_STATEMENT);
        FinancialStatementGenerationStrategyResolver resolver = new FinancialStatementGenerationStrategyResolver(List.of(
                strategy(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION),
                incomeStrategy,
                strategy(EFinancialStatementType.STATEMENT_CHANGES_EQUITY)
        ));

        assertThat(resolver.resolve(EFinancialStatementType.INCOME_STATEMENT)).isSameAs(incomeStrategy);
    }

    @Test
    void shouldFailWhenThereIsNoStrategyForRequestedType() {
        FinancialStatementGenerationStrategyResolver resolver = new FinancialStatementGenerationStrategyResolver(List.of(
                strategy(EFinancialStatementType.INCOME_STATEMENT)
        ));

        assertThatThrownBy(() -> resolver.resolve(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STATEMENT_FINANCIAL_POSITION");
    }

    @Test
    void shouldFailWhenTwoStrategiesSupportTheSameType() {
        assertThatThrownBy(() -> new FinancialStatementGenerationStrategyResolver(List.of(
                strategy(EFinancialStatementType.INCOME_STATEMENT),
                strategy(EFinancialStatementType.INCOME_STATEMENT)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estrategias duplicadas");
    }

    private FinancialStatementGenerationStrategy strategy(EFinancialStatementType type) {
        return new FinancialStatementGenerationStrategy() {
            @Override
            public EFinancialStatementType supports() {
                return type;
            }

            @Override
            public FinancialStatementDataPayload generate(FinancialStatementRequest request) {
                return FinancialStatementDataPayload.builder().build();
            }
        };
    }
}
