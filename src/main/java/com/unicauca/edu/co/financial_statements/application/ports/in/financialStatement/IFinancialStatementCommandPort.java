package com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementHistoryItem;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementLog;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import com.unicauca.edu.co.financial_statements.domain.models.core.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFinancialStatementCommandPort {

    FinancialStatementGenerationResult previewFinancialStatement(FinancialStatementRequest request);

    FinancialStatementGenerationResult registerFinancialStatement(FinancialStatementRequest request);

    Optional<FinancialStatementReport> getFinancialStatementReport(UUID reportId);

    Optional<FinancialStatementGenerationResult> getFinancialStatementSnapshot(UUID reportId);

    PageResult<FinancialStatementHistoryItem> getHistoryByEnterprise(String enterpriseId, int page, int size, String sort);

    List<FinancialStatementLog> getLogsByReportId(UUID reportId);

    FinancialStatementTemplate saveTemplate(FinancialStatementTemplate template);

    FinancialStatementTemplate saveDefaultTemplate(FinancialStatementTemplate template);

    List<FinancialStatementTemplate> getTemplatesByEnterprise(String enterpriseId);

    Optional<FinancialStatementTemplate> getDefaultTemplateByEnterprise(String enterpriseId);

    void registerDeliveryEvent(UUID reportId, String deliveryWay, String message, String eventType);

    void registerLogEvent(UUID reportId, String eventType, String message, String icon, String color);
}
