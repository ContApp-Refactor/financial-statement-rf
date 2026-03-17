package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class ExportFinancialStatementEmailRequest {
    private UUID reportId;
    private String format;
    private String entName;
    private Map<String, Object> financialStatement;
    private List<Map<String, Object>> financialStatementData;
    private InfoReportTemplateRequest infoReportTemplate;

    @Email(message = "toEmail must be a valid email")
    @NotBlank(message = "toEmail is required")
    private String toEmail;
}
