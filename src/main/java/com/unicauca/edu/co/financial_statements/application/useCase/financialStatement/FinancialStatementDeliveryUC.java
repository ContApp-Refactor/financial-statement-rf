package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementDeliveryPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementExportPort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAnnotation;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDocument;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementVisualSignature;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EDeliveryWay;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FinancialStatementDeliveryUC implements IFinancialStatementDeliveryPort {

    private final IFinancialStatementCommandPort financialStatementCommandPort;
    private final IFinancialStatementExportPort financialStatementExportPort;
    private final FinancialStatementReportNameResolver reportNameResolver;
    private final FinancialStatementRowMapper financialStatementRowMapper;
    private final FinancialStatementReportMetadataMapper financialStatementReportMetadataMapper;
    private final FinancialStatementTemplateExportStyleMapper financialStatementTemplateExportStyleMapper;

    @Override
    public FinancialStatementDocument export(FinancialStatementExportCommand command) {
        FinancialStatementExportCommand safeCommand = command != null
                ? command
                : FinancialStatementExportCommand.builder().build();

        ExportContext context = resolveExportContext(
                safeCommand.getReportId(),
                safeCommand.getEnterpriseName(),
                safeCommand.getFinancialStatementData(),
                safeCommand.getFinancialStatement(),
                safeCommand.getExportStyle(),
                safeCommand.getAnnotations(),
                safeCommand.getVisualSignatures()
        );

        IFinancialStatementExportPort.ExportedDocument exportedDocument = financialStatementExportPort.export(
                safeCommand.getFormat() != null ? safeCommand.getFormat() : EReportExportFormat.PDF,
                context.reportName(),
                context.enterpriseName(),
                financialStatementRowMapper.toRowMaps(context.dataRows()),
                context.financialStatement(),
                context.exportStyle(),
                toAnnotationTexts(context.annotations()),
                context.visualSignatures()
        );

        if (safeCommand.getReportId() != null) {
            financialStatementCommandPort.registerDeliveryEvent(
                    safeCommand.getReportId(),
                    EDeliveryWay.DOWNLOAD.name(),
                    "Reporte exportado en formato " + (safeCommand.getFormat() != null ? safeCommand.getFormat().name() : EReportExportFormat.PDF.name()) + ".",
                    "EXPORTED"
            );
        }

        return toDocument(exportedDocument);
    }

    @Override
    public FinancialStatementDocument download(UUID reportId, EReportExportFormat format) {
        if (reportId == null) {
            throw new IllegalArgumentException("El reportId es obligatorio.");
        }

        FinancialStatementGenerationResult snapshot = financialStatementCommandPort.getFinancialStatementSnapshot(reportId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el reporte del estado financiero."));

        if (snapshot.getFinancialStatement() == null) {
            throw new IllegalArgumentException("No se encontro el reporte del estado financiero.");
        }

        FinancialStatementReport report = snapshot.getFinancialStatement();
        IFinancialStatementExportPort.ExportedDocument exportedDocument = financialStatementExportPort.export(
                format != null ? format : EReportExportFormat.PDF,
                reportNameResolver.resolve(report.getType()),
                StringUtils.hasText(report.getEntId()) ? report.getEntId() : "Enterprise",
                financialStatementRowMapper.toRowMaps(snapshot.getFinancialStatementData()),
                buildFinancialStatementMetadata(snapshot),
                resolveDefaultExportStyle(report.getEntId()),
                toAnnotationTexts(snapshot.getAnnotations()),
                null
        );

        financialStatementCommandPort.registerDeliveryEvent(
                reportId,
                EDeliveryWay.DOWNLOAD.name(),
                "Reporte descargado en formato " + (format != null ? format.name() : EReportExportFormat.PDF.name()) + ".",
                "DOWNLOADED"
        );

        return toDocument(exportedDocument);
    }

    private ExportContext resolveExportContext(
            UUID reportId,
            String enterpriseName,
            List<FinancialStatementRow> dataRows,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle requestedExportStyle,
            List<FinancialStatementAnnotation> requestedAnnotations,
            List<FinancialStatementVisualSignature> requestedVisualSignatures
    ) {
        String requestedEnterpriseName = enterpriseName;
        String resolvedEnterpriseName = null;
        String resolvedReportName = null;
        List<FinancialStatementRow> resolvedRows = dataRows != null ? dataRows : List.of();
        List<FinancialStatementAnnotation> resolvedAnnotations = requestedAnnotations != null ? requestedAnnotations : List.of();
        Map<String, Object> resolvedFinancialStatement = financialStatement != null
                ? new LinkedHashMap<>(financialStatement)
                : new LinkedHashMap<>();
        FinancialStatementExportStyle resolvedExportStyle = requestedExportStyle;
        List<FinancialStatementVisualSignature> resolvedVisualSignatures = requestedVisualSignatures != null
                ? requestedVisualSignatures
                : List.of();

        boolean equityChangesRequested = "STATEMENT_CHANGES_EQUITY".equalsIgnoreCase(
                String.valueOf(resolvedFinancialStatement.get("type"))
        );
        boolean requiresSnapshotRows = equityChangesRequested && !containsYearValues(resolvedRows);

        if ((!StringUtils.hasText(resolvedEnterpriseName)
                || resolvedRows.isEmpty()
                || requiresSnapshotRows
                || !resolvedFinancialStatement.containsKey("type")
                || !resolvedFinancialStatement.containsKey("criteria")
                || resolvedFinancialStatement.get("criteria") == null)
                && reportId != null) {
            FinancialStatementGenerationResult snapshot = financialStatementCommandPort
                    .getFinancialStatementSnapshot(reportId)
                    .orElse(null);

            if (snapshot != null) {
                if (resolvedRows.isEmpty() || requiresSnapshotRows) {
                    resolvedRows = snapshot.getFinancialStatementData();
                }
                if (resolvedAnnotations.isEmpty()) {
                    resolvedAnnotations = snapshot.getAnnotations() != null ? snapshot.getAnnotations() : List.of();
                }

                if (snapshot.getFinancialStatement() != null) {
                    resolvedEnterpriseName = snapshot.getFinancialStatement().getEntId();
                    resolvedFinancialStatement.putAll(
                            financialStatementReportMetadataMapper.toMetadataMap(snapshot.getFinancialStatement())
                    );
                    resolvedReportName = reportNameResolver.resolve(snapshot.getFinancialStatement().getType());
                }
            }
        }

        if (!StringUtils.hasText(resolvedReportName)
                && resolvedFinancialStatement.get("type") != null) {
            resolvedReportName = reportNameResolver.resolve(String.valueOf(resolvedFinancialStatement.get("type")));
        }

        if (!StringUtils.hasText(resolvedEnterpriseName)
                && resolvedFinancialStatement.get("entId") != null) {
            resolvedEnterpriseName = String.valueOf(resolvedFinancialStatement.get("entId"));
        }

        if (!StringUtils.hasText(resolvedEnterpriseName)
                && StringUtils.hasText(requestedEnterpriseName)) {
            resolvedEnterpriseName = requestedEnterpriseName;
        }

        if (resolvedExportStyle == null && StringUtils.hasText(resolvedEnterpriseName)) {
            resolvedExportStyle = resolveDefaultExportStyle(resolvedEnterpriseName);
        }

        if (!StringUtils.hasText(resolvedReportName)) {
            resolvedReportName = "Financial Statement";
        }
        if (!StringUtils.hasText(resolvedEnterpriseName)) {
            resolvedEnterpriseName = "Enterprise";
        }

        return new ExportContext(
                resolvedEnterpriseName,
                resolvedReportName,
                resolvedRows,
                resolvedAnnotations,
                resolvedFinancialStatement,
                resolvedExportStyle,
                resolvedVisualSignatures
        );
    }

    private boolean containsYearValues(List<FinancialStatementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }

        return rows.stream()
                .filter(Objects::nonNull)
                .map(FinancialStatementRow::getYearValues)
                .filter(Objects::nonNull)
                .anyMatch(map -> !map.isEmpty());
    }

    private Map<String, Object> buildFinancialStatementMetadata(FinancialStatementGenerationResult snapshot) {
        return snapshot != null
                ? financialStatementReportMetadataMapper.toMetadataMap(snapshot.getFinancialStatement())
                : new LinkedHashMap<>();
    }

    private FinancialStatementDocument toDocument(IFinancialStatementExportPort.ExportedDocument exportedDocument) {
        return FinancialStatementDocument.builder()
                .content(exportedDocument.content())
                .contentType(exportedDocument.contentType())
                .fileName(exportedDocument.fileName())
                .build();
    }

    private FinancialStatementExportStyle resolveDefaultExportStyle(String enterpriseId) {
        return financialStatementCommandPort.getDefaultTemplateByEnterprise(enterpriseId)
                .map(financialStatementTemplateExportStyleMapper::toExportStyle)
                .orElse(null);
    }

    private List<String> toAnnotationTexts(List<FinancialStatementAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }

        return annotations.stream()
                .filter(Objects::nonNull)
                .map(FinancialStatementAnnotation::getText)
                .filter(StringUtils::hasText)
                .toList();
    }

    private record ExportContext(
            String enterpriseName,
            String reportName,
            List<FinancialStatementRow> dataRows,
            List<FinancialStatementAnnotation> annotations,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle exportStyle,
            List<FinancialStatementVisualSignature> visualSignatures
    ) {
    }
}
