package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.mapper;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteriaRange;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailSchedule;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementRowMapper;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.CreateFinancialStatementEmailScheduleRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.ExportFinancialStatementEmailRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.ExportFinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.FinancialStatementCriteriaRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.FinancialStatementCriteriaRangeRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.GenerateFinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.InfoReportTemplateRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.UpsertFinancialStatementTemplateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinancialStatementRestMapper {

    private final FinancialStatementRowMapper financialStatementRowMapper;

    public FinancialStatementRequest toDomain(GenerateFinancialStatementRequest request) {
        if (request == null) {
            return null;
        }

        FinancialStatementCriteriaRequest criteriaRequest = resolveCriteriaRequest(request);
        LocalDate requestedCurrentCutoffDate = criteriaRequest != null
                ? criteriaRequest.getCurrentCutoffDate()
                : null;
        LocalDate requestedPreviousCutoffDate = criteriaRequest != null
                ? criteriaRequest.getPreviousCutoffDate()
                : null;
        LocalDate resolvedStartDate = requestedPreviousCutoffDate != null
                ? requestedPreviousCutoffDate
                : (criteriaRequest != null ? criteriaRequest.getStartDate() : null);
        LocalDate resolvedEndDate = requestedCurrentCutoffDate != null
                ? requestedCurrentCutoffDate
                : (criteriaRequest != null ? criteriaRequest.getEndDate() : null);
        LocalDate resolvedPreviousCutoffDate = requestedPreviousCutoffDate != null
                ? requestedPreviousCutoffDate
                : resolvedStartDate;
        LocalDate resolvedCurrentCutoffDate = requestedCurrentCutoffDate != null
                ? requestedCurrentCutoffDate
                : resolvedEndDate;

        return FinancialStatementRequest.builder()
                .entId(request.getEntId())
                .type(request.getType())
                .criteria(FinancialStatementCriteria.builder()
                        .criteriaType(criteriaRequest != null ? criteriaRequest.getCriteriaType() : null)
                        .criteriaRange(criteriaRequest != null && criteriaRequest.getCriteriaRange() != null
                                ? FinancialStatementCriteriaRange.builder()
                                .from(criteriaRequest.getCriteriaRange().getFrom())
                                .to(criteriaRequest.getCriteriaRange().getTo())
                                .build()
                                : null)
                        .startDate(resolvedStartDate)
                        .endDate(resolvedEndDate)
                        .previousStartDate(criteriaRequest != null ? criteriaRequest.getPreviousStartDate() : null)
                        .previousEndDate(criteriaRequest != null ? criteriaRequest.getPreviousEndDate() : null)
                        .currentCutoffDate(resolvedCurrentCutoffDate)
                        .previousCutoffDate(resolvedPreviousCutoffDate)
                        .build())
                .build();
    }

    public FinancialStatementExportCommand toDomain(ExportFinancialStatementRequest request) {
        if (request == null) {
            return null;
        }

        return FinancialStatementExportCommand.builder()
                .reportId(request.getReportId())
                .format(toExportFormat(request.getFormat()))
                .enterpriseName(request.getEntName())
                .financialStatement(request.getFinancialStatement())
                .financialStatementData(financialStatementRowMapper.toTypedRows(request.getFinancialStatementData()))
                .exportStyle(toExportStyle(request.getInfoReportTemplate()))
                .build();
    }

    public FinancialStatementEmailExportCommand toDomain(ExportFinancialStatementEmailRequest request) {
        if (request == null) {
            return null;
        }

        return FinancialStatementEmailExportCommand.builder()
                .reportId(request.getReportId())
                .format(toExportFormat(request.getFormat()))
                .enterpriseName(request.getEntName())
                .financialStatement(request.getFinancialStatement())
                .financialStatementData(financialStatementRowMapper.toTypedRows(request.getFinancialStatementData()))
                .exportStyle(toExportStyle(request.getInfoReportTemplate()))
                .toEmail(request.getToEmail())
                .build();
    }

    public FinancialStatementExportCommand toPreviewExportCommand(
            FinancialStatementGenerationResult preview,
            String requestedFormat
    ) {
        if (preview == null) {
            return null;
        }

        FinancialStatementReport report = preview.getFinancialStatement();
        Map<String, Object> financialStatement = new LinkedHashMap<>();
        if (report != null) {
            financialStatement.put("reportId", report.getReportId());
            financialStatement.put("type", report.getType() != null ? report.getType().name() : null);
            financialStatement.put("entId", report.getEntId());
            financialStatement.put("criteria", report.getCriteria());
            financialStatement.put("createdAt", report.getCreatedAt());
        }

        return FinancialStatementExportCommand.builder()
                .reportId(report != null ? report.getReportId() : null)
                .format(parseFormat(requestedFormat))
                .enterpriseName(report != null ? report.getEntId() : null)
                .financialStatement(financialStatement)
                .financialStatementData(preview.getFinancialStatementData())
                .build();
    }

    public EReportExportFormat toExportFormat(String requestedFormat) {
        return parseFormat(requestedFormat);
    }

    public FinancialStatementTemplate toDomain(UpsertFinancialStatementTemplateRequest request) {
        if (request == null) {
            return null;
        }

        return FinancialStatementTemplate.builder()
                .id(request.getId())
                .entId(request.getEnterpriseId())
                .name(request.getName())
                .pathLogotype(request.getPathLogotype())
                .alignment(normalizeAlignment(request.getAlignment()))
                .font(request.getFont())
                .fontSize(request.getFontSize())
                .mainColor(request.getMainColor())
                .isDefault(request.getIsDefault())
                .build();
    }

    public FinancialStatementEmailSchedule toDomain(CreateFinancialStatementEmailScheduleRequest request) {
        if (request == null) {
            return null;
        }

        return FinancialStatementEmailSchedule.builder()
                .reportId(request.getReportId())
                .recipientEmail(request.getRecipientEmail())
                .format(request.getFormat() != null ? request.getFormat() : EReportExportFormat.PDF)
                .frequency(request.getFrequency())
                .hourOfDay(request.getHourOfDay())
                .minuteOfHour(request.getMinuteOfHour())
                .dayOfWeek(request.getDayOfWeek())
                .dayOfMonth(request.getDayOfMonth())
                .timezone(request.getTimezone())
                .active(Boolean.TRUE)
                .build();
    }

    private FinancialStatementExportStyle toExportStyle(InfoReportTemplateRequest infoTemplate) {
        if (infoTemplate == null) {
            return null;
        }

        return FinancialStatementExportStyle.builder()
                .pathLogotype(infoTemplate.getPathLogotype())
                .alignment(infoTemplate.getAlienation())
                .font(infoTemplate.getFont())
                .fontSize(infoTemplate.getFontSize())
                .mainColor(infoTemplate.getMainColor())
                .build();
    }

    public FinancialStatementExportStyle toExportStyle(FinancialStatementTemplate template) {
        if (template == null) {
            return null;
        }

        return FinancialStatementExportStyle.builder()
                .pathLogotype(template.getPathLogotype())
                .alignment(normalizeAlignment(template.getAlignment()))
                .font(template.getFont())
                .fontSize(template.getFontSize())
                .mainColor(template.getMainColor())
                .build();
    }

    private EReportExportFormat parseFormat(String requestedFormat) {
        if (!StringUtils.hasText(requestedFormat)) {
            return EReportExportFormat.PDF;
        }

        if ("EXCEL".equalsIgnoreCase(requestedFormat)) {
            return EReportExportFormat.EXCEL;
        }
        return EReportExportFormat.PDF;
    }

    private String normalizeAlignment(String alignment) {
        if (!StringUtils.hasText(alignment)) {
            return null;
        }

        return alignment.trim().toUpperCase();
    }

    private FinancialStatementCriteriaRequest resolveCriteriaRequest(GenerateFinancialStatementRequest request) {
        if (request == null) {
            return null;
        }

        FinancialStatementCriteriaRequest nestedCriteria = request.getCriteria();
        FinancialStatementCriteriaRangeRequest nestedRange = nestedCriteria != null
                ? nestedCriteria.getCriteriaRange()
                : null;

        return FinancialStatementCriteriaRequest.builder()
                .criteriaType(firstNonBlank(
                        nestedCriteria != null ? nestedCriteria.getCriteriaType() : null,
                        request.getCriteriaType()
                ))
                .criteriaRange(nestedRange != null
                        ? nestedRange
                        : request.getCriteriaRange())
                .startDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getStartDate() : null,
                        request.getStartDate()
                ))
                .endDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getEndDate() : null,
                        request.getEndDate()
                ))
                .previousStartDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getPreviousStartDate() : null,
                        request.getPreviousStartDate()
                ))
                .previousEndDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getPreviousEndDate() : null,
                        request.getPreviousEndDate()
                ))
                .currentCutoffDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getCurrentCutoffDate() : null,
                        request.getCurrentCutoffDate()
                ))
                .previousCutoffDate(firstNonNull(
                        nestedCriteria != null ? nestedCriteria.getPreviousCutoffDate() : null,
                        request.getPreviousCutoffDate()
                ))
                .build();
    }

    private String firstNonBlank(String firstValue, String secondValue) {
        if (StringUtils.hasText(firstValue)) {
            return firstValue;
        }
        return StringUtils.hasText(secondValue) ? secondValue : null;
    }

    private LocalDate firstNonNull(LocalDate firstValue, LocalDate secondValue) {
        return firstValue != null ? firstValue : secondValue;
    }
}
