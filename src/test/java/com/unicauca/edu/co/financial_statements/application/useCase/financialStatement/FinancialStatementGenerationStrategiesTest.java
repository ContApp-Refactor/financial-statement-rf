package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.out.IAccountingInfoClient;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementGenerationStrategiesTest {

    private static final String MOCK_ENTERPRISE_ID = "50a69a75-7134-4189-9c62-fc82c53ff679";

    @Test
    void shouldExposeSupportedTypeForEachConcreteStrategy() {
        IAccountingInfoClient accountingInfoClient = inMemoryClient(List.of());

        assertThat(incomeStrategy(accountingInfoClient).supports()).isEqualTo(EFinancialStatementType.INCOME_STATEMENT);
        assertThat(financialPositionStrategy(accountingInfoClient).supports()).isEqualTo(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION);
        assertThat(equityChangesStrategy(accountingInfoClient).supports()).isEqualTo(EFinancialStatementType.STATEMENT_CHANGES_EQUITY);
    }

    @Test
    void shouldGenerateIncomeStatementUsingItsConcreteStrategy() throws IOException {
        List<AccountingEntry> entries = MockAccountingEntryDatasetLoader.load();
        IncomeStatementGenerationStrategy strategy = incomeStrategy(inMemoryClient(entries));

        FinancialStatementDataPayload payload = strategy.generate(FinancialStatementRequest.builder()
                .entId(MOCK_ENTERPRISE_ID)
                .type(EFinancialStatementType.INCOME_STATEMENT)
                .criteria(FinancialStatementCriteria.builder()
                        .criteriaType("ACCOUNT")
                        .startDate(LocalDate.of(2025, 1, 1))
                        .endDate(LocalDate.of(2025, 3, 29))
                        .previousStartDate(LocalDate.of(2024, 1, 1))
                        .previousEndDate(LocalDate.of(2024, 3, 29))
                        .build())
                .build());

        assertThat(payload.getRows()).isNotEmpty();
        assertThat(payload.getRows()).anySatisfy(row -> assertThat(row.getLineDescription()).isEqualTo("Ingresos ordinarios"));
        assertThat(payload.getRows()).anySatisfy(row -> assertThat(row.getLineDescription()).isEqualTo("RESULTADO DEL EJERCICIO"));
    }

    @Test
    void shouldGenerateFinancialPositionUsingItsConcreteStrategy() throws IOException {
        List<AccountingEntry> entries = MockAccountingEntryDatasetLoader.load();
        FinancialPositionGenerationStrategy strategy = financialPositionStrategy(inMemoryClient(entries));

        FinancialStatementDataPayload payload = strategy.generate(FinancialStatementRequest.builder()
                .entId(MOCK_ENTERPRISE_ID)
                .type(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION)
                .criteria(FinancialStatementCriteria.builder()
                        .criteriaType("ACCOUNT")
                        .currentCutoffDate(LocalDate.of(2025, 3, 29))
                        .previousCutoffDate(LocalDate.of(2024, 3, 29))
                        .build())
                .build());

        assertThat(payload.getTotalAssets()).isEqualByComparingTo("724000000.00");
        assertThat(payload.getTotalLiabilities()).isEqualByComparingTo("348000000.00");
        assertThat(payload.getTotalEquity()).isEqualByComparingTo("376000000.00");
        assertThat(payload.getRows()).anySatisfy(row -> assertThat(row.getLineDescription()).isEqualTo("TOTAL PASIVO + PATRIMONIO"));
    }

    @Test
    void shouldGenerateEquityChangesUsingItsConcreteStrategy() throws IOException {
        List<AccountingEntry> entries = MockAccountingEntryDatasetLoader.load();
        EquityChangesGenerationStrategy strategy = equityChangesStrategy(inMemoryClient(entries));

        FinancialStatementDataPayload payload = strategy.generate(FinancialStatementRequest.builder()
                .entId(MOCK_ENTERPRISE_ID)
                .type(EFinancialStatementType.STATEMENT_CHANGES_EQUITY)
                .criteria(FinancialStatementCriteria.builder()
                        .startDate(LocalDate.of(2024, 3, 29))
                        .endDate(LocalDate.of(2025, 3, 29))
                        .build())
                .build());

        assertThat(payload.getRows()).isNotEmpty();
        assertThat(payload.getRows()).anySatisfy(row -> assertThat(row.getLineDescription()).isEqualTo("Capital emitido"));
        assertThat(payload.getRows()).anySatisfy(row -> assertThat(row.getLineDescription()).isEqualTo("Total patrimonio de los accionistas"));
    }

    private IncomeStatementGenerationStrategy incomeStrategy(IAccountingInfoClient accountingInfoClient) {
        AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
        FinancialStatementGenerationSupport generationSupport = new FinancialStatementGenerationSupport(accountingEntryOperations);
        FinancialStatementComparativeRowBuilder comparativeRowBuilder =
                new FinancialStatementComparativeRowBuilder(accountingEntryOperations);
        IncomeStatementAmountCalculator amountCalculator =
                new IncomeStatementAmountCalculator(accountingEntryOperations);
        IncomeStatementEntryClassifier classifier =
                new IncomeStatementEntryClassifier(accountingEntryOperations);
        IncomeStatementStatementBuilder builder =
                new IncomeStatementStatementBuilder(amountCalculator, classifier, comparativeRowBuilder);

        return new IncomeStatementGenerationStrategy(
                accountingInfoClient,
                generationSupport,
                builder,
                new FinancialStatementRowMapper()
        );
    }

    private FinancialPositionGenerationStrategy financialPositionStrategy(IAccountingInfoClient accountingInfoClient) {
        AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
        FinancialStatementGenerationSupport generationSupport = new FinancialStatementGenerationSupport(accountingEntryOperations);
        FinancialStatementComparativeRowBuilder comparativeRowBuilder =
                new FinancialStatementComparativeRowBuilder(accountingEntryOperations);
        IncomeStatementAmountCalculator incomeStatementAmountCalculator =
                new IncomeStatementAmountCalculator(accountingEntryOperations);
        PeriodResultCalculator periodResultCalculator =
                new PeriodResultCalculator(accountingEntryOperations, incomeStatementAmountCalculator);
        FinancialPositionEntryClassifier classifier =
                new FinancialPositionEntryClassifier(accountingEntryOperations);
        FinancialPositionAmountCalculator amountCalculator =
                new FinancialPositionAmountCalculator(classifier, accountingEntryOperations, periodResultCalculator);
        FinancialPositionStatementBuilder builder =
                new FinancialPositionStatementBuilder(amountCalculator, classifier, comparativeRowBuilder, accountingEntryOperations);

        return new FinancialPositionGenerationStrategy(
                accountingInfoClient,
                generationSupport,
                builder,
                new FinancialStatementRowMapper()
        );
    }

    private EquityChangesGenerationStrategy equityChangesStrategy(IAccountingInfoClient accountingInfoClient) {
        AccountingEntryOperations accountingEntryOperations = new AccountingEntryOperations();
        FinancialStatementGenerationSupport generationSupport = new FinancialStatementGenerationSupport(accountingEntryOperations);
        IncomeStatementAmountCalculator incomeStatementAmountCalculator =
                new IncomeStatementAmountCalculator(accountingEntryOperations);
        PeriodResultCalculator periodResultCalculator =
                new PeriodResultCalculator(accountingEntryOperations, incomeStatementAmountCalculator);
        FinancialPositionEntryClassifier classifier =
                new FinancialPositionEntryClassifier(accountingEntryOperations);
        EquityChangesAmountCalculator amountCalculator =
                new EquityChangesAmountCalculator(accountingEntryOperations, classifier, periodResultCalculator);
        EquityChangesStatementBuilder builder =
                new EquityChangesStatementBuilder(amountCalculator, new FinancialStatementComparativeRowBuilder(accountingEntryOperations));

        return new EquityChangesGenerationStrategy(
                accountingInfoClient,
                generationSupport,
                builder,
                new FinancialStatementRowMapper()
        );
    }

    private IAccountingInfoClient inMemoryClient(List<AccountingEntry> entries) {
        return new IAccountingInfoClient() {
            @Override
            public List<AccountingEntry> findAccountingEntries(String entId, LocalDate startDate, LocalDate endDate) {
                return entries.stream()
                        .filter(entry -> entry != null)
                        .filter(entry -> entId == null || entId.equalsIgnoreCase(entry.getEntId()))
                        .filter(entry -> startDate == null || (entry.getDate() != null && !entry.getDate().isBefore(startDate)))
                        .filter(entry -> endDate == null || (entry.getDate() != null && !entry.getDate().isAfter(endDate)))
                        .toList();
            }
        };
    }
}
