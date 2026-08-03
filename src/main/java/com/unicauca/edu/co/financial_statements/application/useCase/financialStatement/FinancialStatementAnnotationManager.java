package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementPersistencePort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAnnotation;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementAnnotationEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FinancialStatementAnnotationManager {

    private static final String EMPTY_ANNOTATION_MESSAGE = "Debe escribir una anotacion antes de guardar.";

    private final IFinancialStatementPersistencePort financialStatementPersistencePort;

    public List<FinancialStatementAnnotation> getAnnotations(UUID reportId) {
        validateReportId(reportId);

        return financialStatementPersistencePort.findAnnotationsByReportId(reportId).stream()
                .map(entity -> toDomain(entity, reportId))
                .toList();
    }

    public FinancialStatementAnnotation createAnnotation(UUID reportId, String text) {
        FinancialStatementEntity statement = resolveReport(reportId);
        String normalizedText = normalizeText(text);
        OffsetDateTime now = OffsetDateTime.now();

        return toDomain(financialStatementPersistencePort.saveAnnotation(
                FinancialStatementAnnotationEntity.builder()
                        .financialStatement(statement)
                        .text(normalizedText)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        ), reportId);
    }

    public FinancialStatementAnnotation updateAnnotation(UUID reportId, Long annotationId, String text) {
        validateReportId(reportId);
        if (annotationId == null) {
            throw new IllegalArgumentException("El annotationId es obligatorio.");
        }

        FinancialStatementAnnotationEntity entity = financialStatementPersistencePort
                .findAnnotationByIdAndReportId(annotationId, reportId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la anotacion del estado financiero."));

        entity.setText(normalizeText(text));
        entity.setUpdatedAt(OffsetDateTime.now());
        return toDomain(financialStatementPersistencePort.saveAnnotation(entity), reportId);
    }

    public void deleteAnnotation(UUID reportId, Long annotationId) {
        validateReportId(reportId);
        if (annotationId == null) {
            throw new IllegalArgumentException("El annotationId es obligatorio.");
        }

        FinancialStatementAnnotationEntity entity = financialStatementPersistencePort
                .findAnnotationByIdAndReportId(annotationId, reportId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la anotacion del estado financiero."));

        financialStatementPersistencePort.deleteAnnotation(entity);
    }

    private FinancialStatementEntity resolveReport(UUID reportId) {
        validateReportId(reportId);

        return financialStatementPersistencePort.findFinancialStatementByReportId(reportId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el reporte del estado financiero."));
    }

    private void validateReportId(UUID reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("El reportId es obligatorio.");
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(EMPTY_ANNOTATION_MESSAGE);
        }
        return text.trim();
    }

    private FinancialStatementAnnotation toDomain(FinancialStatementAnnotationEntity entity, UUID reportId) {
        return FinancialStatementAnnotation.builder()
                .id(entity.getId())
                .reportId(reportId)
                .text(entity.getText())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
