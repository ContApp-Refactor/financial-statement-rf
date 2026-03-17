package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

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
public class ExportFinancialStatementRequest {
    private UUID reportId;
    private String format;
    private String entName;
    private Map<String, Object> financialStatement;
    private List<Map<String, Object>> financialStatementData;
    private InfoReportTemplateRequest infoReportTemplate;
}
