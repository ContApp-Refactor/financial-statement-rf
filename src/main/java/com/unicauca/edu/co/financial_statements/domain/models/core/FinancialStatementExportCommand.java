package com.unicauca.edu.co.financial_statements.domain.models.core;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementExportCommand {
    private UUID reportId;
    private EReportExportFormat format;
    private String enterpriseName;
    private Map<String, Object> financialStatement;
    private List<FinancialStatementRow> financialStatementData;
    private List<FinancialStatementAnnotation> annotations;
    private FinancialStatementVisualSignature visualSignature;
    private FinancialStatementExportStyle exportStyle;
}
