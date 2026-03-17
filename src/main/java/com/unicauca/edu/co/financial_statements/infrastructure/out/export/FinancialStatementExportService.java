package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinancialStatementExportService {

    private static final String DEFAULT_TITLE = "Financial Statement";
    private static final String DEFAULT_ENTERPRISE = "Enterprise";

    private final FinancialStatementExportRowMapper financialStatementExportRowMapper;
    private final FinancialStatementTableModelResolver financialStatementTableModelResolver;
    private final FinancialStatementDocumentRenderer financialStatementDocumentRenderer;

    public ExportedFile export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows
    ) {
        return export(format, reportName, enterpriseName, rows, null, null);
    }

    public ExportedFile export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows,
            Map<String, Object> financialStatement,
            ExportStyle exportStyle
    ) {
        EReportExportFormat safeFormat = format != null ? format : EReportExportFormat.PDF;
        String safeReportName = StringUtils.hasText(reportName) ? reportName.trim() : DEFAULT_TITLE;
        String safeEnterprise = StringUtils.hasText(enterpriseName) ? enterpriseName.trim() : DEFAULT_ENTERPRISE;
        List<Map<String, Object>> safeRows = rows != null ? rows : List.of();
        List<FinancialStatementRow> typedRows = financialStatementExportRowMapper.toRows(safeRows);
        Map<String, Object> safeFinancialStatement = financialStatement != null ? financialStatement : Map.of();
        ExportStyle safeExportStyle = exportStyle != null ? exportStyle : new ExportStyle(null, null, null, null, null);

        FinancialStatementTableModel model = financialStatementTableModelResolver.resolve(
                safeReportName,
                safeEnterprise,
                safeRows,
                typedRows,
                safeFinancialStatement
        );

        return financialStatementDocumentRenderer.export(
                safeFormat,
                model,
                safeExportStyle
        );
    }

    public record ExportStyle(
            String pathLogotype,
            String alignment,
            String font,
            Integer fontSize,
            String mainColor
    ) {
    }

    public record ExportedFile(
            byte[] content,
            MediaType mediaType,
            String fileName,
            EReportExportFormat format
    ) {
    }
}
