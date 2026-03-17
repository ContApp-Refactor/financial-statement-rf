package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;

public interface FinancialStatementDocumentRenderer {

    FinancialStatementExportService.ExportedFile export(
            EReportExportFormat format,
            FinancialStatementTableModel model,
            FinancialStatementExportService.ExportStyle exportStyle
    );
}
