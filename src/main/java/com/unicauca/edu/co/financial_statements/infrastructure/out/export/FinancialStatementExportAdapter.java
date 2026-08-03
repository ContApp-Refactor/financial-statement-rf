package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementExportPort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementVisualSignature;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinancialStatementExportAdapter implements IFinancialStatementExportPort {

    private final FinancialStatementExportService financialStatementExportService;

    @Override
    public ExportedDocument export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows
    ) {
        FinancialStatementExportService.ExportedFile exportedFile = financialStatementExportService.export(
                format,
                reportName,
                enterpriseName,
                rows
        );

        return new ExportedDocument(
                exportedFile.content(),
                exportedFile.mediaType().toString(),
                exportedFile.fileName()
        );
    }

    @Override
    public ExportedDocument export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle exportStyle
    ) {
        return export(format, reportName, enterpriseName, rows, financialStatement, exportStyle, List.of(), null);
    }

    @Override
    public ExportedDocument export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle exportStyle,
            List<String> annotationTexts,
            List<FinancialStatementVisualSignature> visualSignatures
    ) {
        FinancialStatementExportService.ExportedFile exportedFile = financialStatementExportService.export(
                format,
                reportName,
                enterpriseName,
                rows,
                financialStatement,
                exportStyle != null
                        ? new FinancialStatementExportService.ExportStyle(
                        exportStyle.getPathLogotype(),
                        exportStyle.getAlignment(),
                        exportStyle.getFont(),
                        exportStyle.getFontSize(),
                        exportStyle.getMainColor()
                )
                        : null,
                annotationTexts,
                visualSignatures
        );

        return new ExportedDocument(
                exportedFile.content(),
                exportedFile.mediaType().toString(),
                exportedFile.fileName()
        );
    }
}
