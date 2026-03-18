package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class FinancialStatementReportMetadataMapper {

    public Map<String, Object> toMetadataMap(FinancialStatementReport report) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (report == null) {
            return metadata;
        }

        metadata.put("reportId", report.getReportId());
        metadata.put("type", report.getType() != null ? report.getType().name() : null);
        metadata.put("entId", report.getEntId());
        metadata.put("criteria", report.getCriteria());
        metadata.put("createdAt", report.getCreatedAt());
        return metadata;
    }

    public String buildDownloadUrl(UUID reportId) {
        return reportId != null ? "/api/financial-statements/" + reportId + "/download" : null;
    }
}
