package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementExportCriteriaResolverTest {

    private final FinancialStatementExportCriteriaResolver resolver =
            new FinancialStatementExportCriteriaResolver(new ObjectMapper());

    @Test
    void shouldResolveIncomeStatementCriteriaFromDomainCriteriaObject() {
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "INCOME_STATEMENT");
        financialStatement.put("criteria", FinancialStatementCriteria.builder()
                .criteriaType("SUB_ACCOUNT")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 3, 29))
                .previousStartDate(LocalDate.of(2024, 1, 1))
                .previousEndDate(LocalDate.of(2024, 3, 29))
                .build());

        List<ReportCriterion> criteria = resolver.resolveCriteria(financialStatement);

        assertThat(criteria)
                .containsExactly(
                        new ReportCriterion("Tipo de Nivel", "Subcuenta"),
                        new ReportCriterion("Fecha de Inicio Periodo Actual", "01/01/2025"),
                        new ReportCriterion("Fecha de Fin Periodo Actual", "29/03/2025"),
                        new ReportCriterion("Fecha de Inicio Periodo Anterior", "01/01/2024"),
                        new ReportCriterion("Fecha de Fin Periodo Anterior", "29/03/2024")
                );
    }

    @Test
    void shouldResolveFinancialPositionCriteriaUsingCutoffDates() {
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "STATEMENT_FINANCIAL_POSITION");
        financialStatement.put("criteria", Map.of(
                "criteriaType", "GROUP",
                "currentCutoffDate", "2025-03-29",
                "previousCutoffDate", "2024-03-29"
        ));

        List<ReportCriterion> criteria = resolver.resolveCriteria(financialStatement);

        assertThat(criteria)
                .containsExactly(
                        new ReportCriterion("Tipo de Nivel", "Grupo"),
                        new ReportCriterion("Fecha de Corte Anterior", "29/03/2024"),
                        new ReportCriterion("Fecha de Corte Actual", "29/03/2025")
                );
    }

    @Test
    void shouldUseDefaultLevelLabelWhenCriteriaTypeIsMissing() {
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "STATEMENT_FINANCIAL_POSITION");
        financialStatement.put("criteria", Map.of(
                "currentCutoffDate", "2025-03-29",
                "previousCutoffDate", "2024-03-29"
        ));

        List<ReportCriterion> criteria = resolver.resolveCriteria(financialStatement);

        assertThat(criteria)
                .containsExactly(
                        new ReportCriterion("Tipo de Nivel", "Estructura predeterminada"),
                        new ReportCriterion("Fecha de Corte Anterior", "29/03/2024"),
                        new ReportCriterion("Fecha de Corte Actual", "29/03/2025")
                );
    }

    @Test
    void shouldExtractYearFromIsoDateOrReturnNullForMissingValue() {
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("criteria", Map.of(
                "startDate", "2025-01-01",
                "previousEndDate", ""
        ));

        assertThat(resolver.extractYearFromCriteria(financialStatement, "startDate")).isEqualTo(2025);
        assertThat(resolver.extractYearFromCriteria(financialStatement, "previousEndDate")).isNull();
        assertThat(resolver.extractYearFromCriteria(financialStatement, "currentCutoffDate")).isNull();
    }
}
