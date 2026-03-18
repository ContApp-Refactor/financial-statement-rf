package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.out.IAccountingInfoClient;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinancialPositionGenerationStrategy implements FinancialStatementGenerationStrategy {

    private final IAccountingInfoClient accountingInfoClient;
    private final FinancialStatementGenerationSupport generationSupport;
    private final FinancialPositionStatementBuilder financialPositionStatementBuilder;
    private final FinancialStatementRowMapper financialStatementRowMapper;

    @Override
    public EFinancialStatementType supports() {
        return EFinancialStatementType.STATEMENT_FINANCIAL_POSITION;
    }

    @Override
    public FinancialStatementDataPayload generate(FinancialStatementRequest request) {
        FinancialStatementCriteria criteria = request != null ? request.getCriteria() : null;
        LocalDate currentCutoffDate = criteria != null && criteria.getCurrentCutoffDate() != null
                ? criteria.getCurrentCutoffDate()
                : (criteria != null ? criteria.getEndDate() : null);
        LocalDate previousCutoffDate = criteria != null && criteria.getPreviousCutoffDate() != null
                ? criteria.getPreviousCutoffDate()
                : (criteria != null ? criteria.getStartDate() : null);

        List<AccountingEntry> sourceEntries = accountingInfoClient.findAccountingEntries(request.getEntId(), null, currentCutoffDate);
        List<AccountingEntry> filteredSourceEntries = generationSupport.applyCriteriaLevelFilter(sourceEntries, criteria, true);

        generationSupport.validateCutoffDatesAgainstLatestMovement(
                filteredSourceEntries,
                currentCutoffDate,
                previousCutoffDate
        );

        List<AccountingEntry> currentAccountingEntries =
                generationSupport.filterEntriesUpToCutoff(filteredSourceEntries, currentCutoffDate);
        List<AccountingEntry> previousAccountingEntries =
                generationSupport.filterEntriesUpToCutoff(filteredSourceEntries, previousCutoffDate);

        FinancialPositionRowsResult result = financialPositionStatementBuilder.build(
                currentAccountingEntries,
                previousAccountingEntries,
                criteria,
                currentCutoffDate,
                previousCutoffDate
        );

        generationSupport.validateBalancedFinancialPosition(
                result.totalAssets(),
                result.totalLiabilities(),
                result.totalEquity(),
                currentCutoffDate,
                "current"
        );
        generationSupport.validateBalancedFinancialPosition(
                result.previousTotalAssets(),
                result.previousTotalLiabilities(),
                result.previousTotalEquity(),
                previousCutoffDate,
                "previous"
        );

        return FinancialStatementDataPayload.builder()
                .rows(financialStatementRowMapper.toTypedRows(result.rows()))
                .totalAssets(result.totalAssets())
                .totalLiabilities(result.totalLiabilities())
                .totalEquity(result.totalEquity())
                .build();
    }
}
