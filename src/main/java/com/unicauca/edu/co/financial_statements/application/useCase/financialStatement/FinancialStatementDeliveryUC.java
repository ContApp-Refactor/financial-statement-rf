package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementDeliveryPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementExportPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementMailPort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDocument;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
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
    private final IFinancialStatementMailPort financialStatementMailPort;
    private final FinancialStatementReportNameResolver reportNameResolver;
    private final FinancialStatementRowMapper financialStatementRowMapper;

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
                safeCommand.getExportStyle()
        );

        IFinancialStatementExportPort.ExportedDocument exportedDocument = financialStatementExportPort.export(
                safeCommand.getFormat() != null ? safeCommand.getFormat() : EReportExportFormat.PDF,
                context.reportName(),
                context.enterpriseName(),
                financialStatementRowMapper.toRowMaps(context.dataRows()),
                context.financialStatement(),
                context.exportStyle()
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
    public void exportByEmail(FinancialStatementEmailExportCommand command) throws Exception {
        if (command == null || !StringUtils.hasText(command.getToEmail())) {
            throw new IllegalArgumentException("toEmail is required.");
        }

        ExportContext context = resolveExportContext(
                command.getReportId(),
                command.getEnterpriseName(),
                command.getFinancialStatementData(),
                command.getFinancialStatement(),
                command.getExportStyle()
        );

        IFinancialStatementExportPort.ExportedDocument exportedDocument = financialStatementExportPort.export(
                command.getFormat() != null ? command.getFormat() : EReportExportFormat.PDF,
                context.reportName(),
                context.enterpriseName(),
                financialStatementRowMapper.toRowMaps(context.dataRows()),
                context.financialStatement(),
                context.exportStyle()
        );

        financialStatementMailPort.sendReport(
                command.getToEmail(),
                "Financial Statement Export",
                "Adjunto encontraras el reporte financiero solicitado.",
                exportedDocument.content(),
                exportedDocument.fileName(),
                exportedDocument.contentType()
        );

        if (command.getReportId() != null) {
            financialStatementCommandPort.registerDeliveryEvent(
                    command.getReportId(),
                    EDeliveryWay.EMAIL.name(),
                    "Reporte enviado por correo a " + command.getToEmail() + ".",
                    "EMAILED"
            );
        }

    }

    @Override
    public FinancialStatementDocument download(UUID reportId, EReportExportFormat format) {
        if (reportId == null) {
            throw new IllegalArgumentException("reportId is required.");
        }

        FinancialStatementGenerationResult snapshot = financialStatementCommandPort.getFinancialStatementSnapshot(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Financial statement report not found."));

        if (snapshot.getFinancialStatement() == null) {
            throw new IllegalArgumentException("Financial statement report not found.");
        }

        FinancialStatementReport report = snapshot.getFinancialStatement();
        IFinancialStatementExportPort.ExportedDocument exportedDocument = financialStatementExportPort.export(
                format != null ? format : EReportExportFormat.PDF,
                reportNameResolver.resolve(report.getType()),
                StringUtils.hasText(report.getEntId()) ? report.getEntId() : "Enterprise",
                financialStatementRowMapper.toRowMaps(snapshot.getFinancialStatementData()),
                buildFinancialStatementMetadata(snapshot),
                resolveDefaultExportStyle(report.getEntId())
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
            FinancialStatementExportStyle requestedExportStyle
    ) {
        String requestedEnterpriseName = enterpriseName;
        String resolvedEnterpriseName = null;
        String resolvedReportName = null;
        List<FinancialStatementRow> resolvedRows = dataRows != null ? dataRows : List.of();
        Map<String, Object> resolvedFinancialStatement = financialStatement != null
                ? new LinkedHashMap<>(financialStatement)
                : new LinkedHashMap<>();
        FinancialStatementExportStyle resolvedExportStyle = requestedExportStyle;

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

                if (snapshot.getFinancialStatement() != null) {
                    resolvedEnterpriseName = snapshot.getFinancialStatement().getEntId();
                    resolvedFinancialStatement.putAll(buildFinancialStatementMetadata(snapshot));
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
                resolvedFinancialStatement,
                resolvedExportStyle
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
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (snapshot == null || snapshot.getFinancialStatement() == null) {
            return metadata;
        }

        FinancialStatementReport report = snapshot.getFinancialStatement();
        metadata.put("reportId", report.getReportId());
        metadata.put("type", report.getType() != null ? report.getType().name() : null);
        metadata.put("entId", report.getEntId());
        metadata.put("criteria", report.getCriteria());
        metadata.put("createdAt", report.getCreatedAt());
        return metadata;
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
                .map(this::toExportStyle)
                .orElse(null);
    }

    private FinancialStatementExportStyle toExportStyle(FinancialStatementTemplate template) {
        if (template == null) {
            return null;
        }

        return FinancialStatementExportStyle.builder()
                .pathLogotype(template.getPathLogotype())
                .alignment(template.getAlignment())
                .font(template.getFont())
                .fontSize(template.getFontSize())
                .mainColor(template.getMainColor())
                .build();
    }

    private record ExportContext(
            String enterpriseName,
            String reportName,
            List<FinancialStatementRow> dataRows,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle exportStyle
    ) {
    }
}
