package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialStatementDataGeneratorTest {

    @Mock
    private FinancialStatementGenerationStrategyResolver strategyResolver;

    @Mock
    private FinancialStatementGenerationStrategy strategy;

    @InjectMocks
    private FinancialStatementDataGenerator dataGenerator;

    @Test
    void shouldDelegateGenerationToResolvedStrategy() {
        FinancialStatementRequest request = FinancialStatementRequest.builder()
                .type(EFinancialStatementType.INCOME_STATEMENT)
                .build();
        FinancialStatementDataPayload expectedPayload = FinancialStatementDataPayload.builder().build();

        when(strategyResolver.resolve(EFinancialStatementType.INCOME_STATEMENT)).thenReturn(strategy);
        when(strategy.generate(request)).thenReturn(expectedPayload);

        FinancialStatementDataPayload payload = dataGenerator.generate(request);

        assertThat(payload).isSameAs(expectedPayload);
        verify(strategyResolver).resolve(EFinancialStatementType.INCOME_STATEMENT);
        verify(strategy).generate(request);
    }

    @Test
    void shouldRejectRequestsWithoutTypeBeforeResolvingStrategy() {
        FinancialStatementRequest request = FinancialStatementRequest.builder().build();

        assertThatThrownBy(() -> dataGenerator.generate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request type is required");
    }
}
