package com.unicauca.edu.co.financial_statements.application.ports.out;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;

import java.util.List;
import java.util.Map;

public interface IFinancialStatementExportPort {

    ExportedDocument export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows
    );

    default ExportedDocument export(
            EReportExportFormat format,
            String reportName,
            String enterpriseName,
            List<Map<String, Object>> rows,
            Map<String, Object> financialStatement,
            FinancialStatementExportStyle exportStyle
    ) {
        return export(format, reportName, enterpriseName, rows);
    }

    record ExportedDocument(
            byte[] content,
            String contentType,
            String fileName
    ) {
    }
}
