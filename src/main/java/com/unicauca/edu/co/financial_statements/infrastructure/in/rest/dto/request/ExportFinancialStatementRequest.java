package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
    private List<UpsertFinancialStatementAnnotationRequest> annotations;
    @Valid
    private VisualSignatureRequest signature;
    @Valid
    @Size(max = 2, message = "Solo se permiten hasta 2 firmas.")
    private List<VisualSignatureRequest> signatures;
    private InfoReportTemplateRequest infoReportTemplate;
}
