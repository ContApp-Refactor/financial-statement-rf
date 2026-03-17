package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteriaRange;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class FinancialStatementRequestSupport {

    public FinancialStatementRequest normalizeRequest(FinancialStatementRequest request) {
        if (request == null) {
            return null;
        }

        return FinancialStatementRequest.builder()
                .entId(StringUtils.hasText(request.getEntId()) ? request.getEntId().trim() : request.getEntId())
                .type(request.getType())
                .criteria(normalizeCriteria(request.getCriteria()))
                .build();
    }

    public void validateRequest(FinancialStatementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The request cannot be null.");
        }

        if (!StringUtils.hasText(request.getEntId())) {
            throw new IllegalArgumentException("entId is required.");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("type is required.");
        }

        FinancialStatementCriteria criteria = request.getCriteria();
        if (criteria == null) {
            throw new IllegalArgumentException("criteria is required.");
        }

        LocalDate startDate = criteria.getStartDate();
        LocalDate endDate = criteria.getEndDate();
        LocalDate currentCutoffDate = criteria.getCurrentCutoffDate() != null
                ? criteria.getCurrentCutoffDate()
                : endDate;
        LocalDate previousCutoffDate = criteria.getPreviousCutoffDate() != null
                ? criteria.getPreviousCutoffDate()
                : startDate;

        if (request.getType() == EFinancialStatementType.STATEMENT_FINANCIAL_POSITION) {
            if (currentCutoffDate == null || previousCutoffDate == null) {
                throw new IllegalArgumentException("currentCutoffDate and previousCutoffDate are required for statement financial position.");
            }
            if (previousCutoffDate.isAfter(currentCutoffDate)) {
                throw new IllegalArgumentException("previousCutoffDate cannot be after currentCutoffDate.");
            }
        } else if (request.getType() == EFinancialStatementType.INCOME_STATEMENT) {
            LocalDate previousStartDate = criteria.getPreviousStartDate();
            LocalDate previousEndDate = criteria.getPreviousEndDate();

            if (startDate == null || endDate == null || previousStartDate == null || previousEndDate == null) {
                throw new IllegalArgumentException("Current and previous period dates are required for income statement.");
            }
        } else if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required for this report type.");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate.");
        }

        if (criteria.getPreviousStartDate() != null
                && criteria.getPreviousEndDate() != null
                && criteria.getPreviousStartDate().isAfter(criteria.getPreviousEndDate())) {
            throw new IllegalArgumentException("previousStartDate cannot be after previousEndDate.");
        }

        FinancialStatementCriteriaRange criteriaRange = criteria.getCriteriaRange();
        if (criteriaRange != null
                && criteriaRange.getFrom() != null
                && criteriaRange.getTo() != null
                && criteriaRange.getFrom() > criteriaRange.getTo()) {
            throw new IllegalArgumentException("criteriaRange.from cannot be greater than criteriaRange.to.");
        }
    }

    public FinancialStatementCriteria normalizePersistedCriteria(
            EFinancialStatementType type,
            FinancialStatementCriteria criteria
    ) {
        if (criteria == null) {
            return null;
        }

        return switch (type) {
            case STATEMENT_FINANCIAL_POSITION -> FinancialStatementCriteria.builder()
                    .criteriaType(criteria.getCriteriaType())
                    .criteriaRange(criteria.getCriteriaRange())
                    .startDate(firstNonNull(criteria.getPreviousCutoffDate(), criteria.getStartDate()))
                    .endDate(firstNonNull(criteria.getCurrentCutoffDate(), criteria.getEndDate()))
                    .previousCutoffDate(firstNonNull(criteria.getPreviousCutoffDate(), criteria.getStartDate()))
                    .currentCutoffDate(firstNonNull(criteria.getCurrentCutoffDate(), criteria.getEndDate()))
                    .build();
            case INCOME_STATEMENT -> FinancialStatementCriteria.builder()
                    .criteriaType(criteria.getCriteriaType())
                    .criteriaRange(criteria.getCriteriaRange())
                    .startDate(criteria.getStartDate())
                    .endDate(criteria.getEndDate())
                    .previousStartDate(criteria.getPreviousStartDate())
                    .previousEndDate(criteria.getPreviousEndDate())
                    .build();
            case STATEMENT_CHANGES_EQUITY -> FinancialStatementCriteria.builder()
                    .criteriaType(criteria.getCriteriaType())
                    .criteriaRange(criteria.getCriteriaRange())
                    .startDate(criteria.getStartDate())
                    .endDate(criteria.getEndDate())
                    .build();
        };
    }

    private FinancialStatementCriteria normalizeCriteria(FinancialStatementCriteria criteria) {
        if (criteria == null) {
            return null;
        }

        String normalizedCriteriaType = StringUtils.hasText(criteria.getCriteriaType())
                ? criteria.getCriteriaType().trim().toUpperCase(Locale.ROOT)
                : null;

        return FinancialStatementCriteria.builder()
                .criteriaType(normalizedCriteriaType)
                .criteriaRange(normalizeCriteriaRange(normalizedCriteriaType, criteria.getCriteriaRange()))
                .startDate(criteria.getStartDate())
                .endDate(criteria.getEndDate())
                .previousStartDate(criteria.getPreviousStartDate())
                .previousEndDate(criteria.getPreviousEndDate())
                .currentCutoffDate(criteria.getCurrentCutoffDate())
                .previousCutoffDate(criteria.getPreviousCutoffDate())
                .build();
    }

    private FinancialStatementCriteriaRange normalizeCriteriaRange(
            String criteriaType,
            FinancialStatementCriteriaRange criteriaRange
    ) {
        if (!StringUtils.hasText(criteriaType) || criteriaRange == null) {
            return null;
        }
        if (criteriaRange.getFrom() == null && criteriaRange.getTo() == null) {
            return null;
        }

        return FinancialStatementCriteriaRange.builder()
                .from(criteriaRange.getFrom())
                .to(criteriaRange.getTo())
                .build();
    }

    private <T> T firstNonNull(T primaryValue, T fallbackValue) {
        return primaryValue != null ? primaryValue : fallbackValue;
    }
}
