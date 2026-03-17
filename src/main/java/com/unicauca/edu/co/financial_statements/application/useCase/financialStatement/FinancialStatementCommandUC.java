package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementPersistencePort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementHistoryItem;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementLog;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import com.unicauca.edu.co.financial_statements.domain.models.core.PageResult;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EDeliveryWay;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementHistoryEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FinancialStatementCommandUC implements IFinancialStatementCommandPort {

    private final IFinancialStatementPersistencePort financialStatementPersistencePort;
    private final FinancialStatementSnapshotMapper financialStatementSnapshotMapper;
    private final FinancialStatementRequestSupport financialStatementRequestSupport;
    private final FinancialStatementTemplateManager financialStatementTemplateManager;
    private final FinancialStatementDataGenerator financialStatementDataGenerator;

    @Override
    @Transactional(readOnly = true)
    public FinancialStatementGenerationResult previewFinancialStatement(FinancialStatementRequest request) {
        FinancialStatementRequest normalizedRequest = financialStatementRequestSupport.normalizeRequest(request);
        financialStatementRequestSupport.validateRequest(normalizedRequest);
        return generateFinancialStatement(normalizedRequest, false);
    }

    @Override
    @Transactional
    public FinancialStatementGenerationResult registerFinancialStatement(FinancialStatementRequest request) {
        FinancialStatementRequest normalizedRequest = financialStatementRequestSupport.normalizeRequest(request);
        financialStatementRequestSupport.validateRequest(normalizedRequest);
        return generateFinancialStatement(normalizedRequest, true);
    }

    @Override
    public Optional<FinancialStatementReport> getFinancialStatementReport(UUID reportId) {
        return financialStatementPersistencePort.findFinancialStatementByReportId(reportId)
                .map(this::toDomain);
    }

    @Override
    public Optional<FinancialStatementGenerationResult> getFinancialStatementSnapshot(UUID reportId) {
        return financialStatementPersistencePort.findFinancialStatementByReportId(reportId)
                .map(financialStatementSnapshotMapper::toGenerationResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinancialStatementHistoryItem> getHistoryByEnterprise(String enterpriseId, int page, int size, String sort) {
        if (!StringUtils.hasText(enterpriseId)) {
            throw new IllegalArgumentException("enterpriseId is required.");
        }

        Pageable pageable = buildHistoryPageable(page, size, sort);
        Page<FinancialStatementHistoryEntity> historyPage = financialStatementPersistencePort
                .findHistoryByEnterprise(enterpriseId, pageable);

        return PageResult.<FinancialStatementHistoryItem>builder()
                .content(historyPage.getContent().stream().map(this::toHistoryItem).toList())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialStatementLog> getLogsByReportId(UUID reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("reportId is required.");
        }

        return financialStatementPersistencePort.findLogsByReportId(reportId).stream()
                .map(this::toLog)
                .toList();
    }

    @Override
    @Transactional
    public FinancialStatementTemplate saveTemplate(FinancialStatementTemplate template) {
        return financialStatementTemplateManager.saveTemplate(template);
    }

    @Override
    @Transactional
    public FinancialStatementTemplate saveDefaultTemplate(FinancialStatementTemplate template) {
        return financialStatementTemplateManager.saveDefaultTemplate(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialStatementTemplate> getTemplatesByEnterprise(String enterpriseId) {
        return financialStatementTemplateManager.getTemplatesByEnterprise(enterpriseId);
    }

    @Override
    public Optional<FinancialStatementTemplate> getDefaultTemplateByEnterprise(String enterpriseId) {
        return financialStatementTemplateManager.getDefaultTemplateByEnterprise(enterpriseId);
    }

    @Override
    @Transactional
    public void registerDeliveryEvent(UUID reportId, String deliveryWay, String message, String eventType) {
        FinancialStatementEntity statement = resolveFinancialStatement(reportId);
        OffsetDateTime eventAt = OffsetDateTime.now();
        String resolvedDeliveryWay = StringUtils.hasText(deliveryWay)
                ? deliveryWay.trim().toUpperCase()
                : EDeliveryWay.DOWNLOAD.name();
        String resolvedEventType = StringUtils.hasText(eventType)
                ? eventType.trim().toUpperCase()
                : "DELIVERED";
        String resolvedState = "EMAIL".equals(resolvedDeliveryWay) || "SCHEDULED_EMAIL".equals(resolvedDeliveryWay)
                ? "EMAILED"
                : "DOWNLOADED";
        String resolvedMessage = StringUtils.hasText(message)
                ? message
                : ("EMAILED".equals(resolvedState)
                ? "Reporte enviado por correo."
                : "Reporte descargado correctamente.");

        appendHistory(statement, resolvedState, resolvedDeliveryWay, eventAt);
        appendLog(
                statement,
                resolvedEventType,
                resolvedMessage,
                "EMAIL".equals(resolvedDeliveryWay) || "SCHEDULED_EMAIL".equals(resolvedDeliveryWay) ? "mail" : "download",
                "INFO",
                eventAt
        );
    }

    @Override
    @Transactional
    public void registerLogEvent(UUID reportId, String eventType, String message, String icon, String color) {
        FinancialStatementEntity statement = resolveFinancialStatement(reportId);
        appendLog(statement, eventType, message, icon, color, OffsetDateTime.now());
    }

    private FinancialStatementGenerationResult generateFinancialStatement(
            FinancialStatementRequest request,
            boolean persist
    ) {
        OffsetDateTime createdAt = OffsetDateTime.now();
        FinancialStatementCriteria persistedCriteria = financialStatementRequestSupport.normalizePersistedCriteria(
                request.getType(),
                request.getCriteria()
        );
        UUID reportId = persist ? UUID.randomUUID() : null;

        FinancialStatementReport report = FinancialStatementReport.builder()
                .reportId(reportId)
                .type(request.getType())
                .entId(request.getEntId())
                .criteria(persistedCriteria)
                .createdAt(createdAt)
                .downloadUrl(persist ? buildDownloadUrl(reportId) : null)
                .build();

        FinancialStatementDataPayload payload = financialStatementDataGenerator.generate(request);

        if (persist) {
            FinancialStatementEntity savedStatement = financialStatementPersistencePort.saveFinancialStatement(
                    toEntity(report, financialStatementSnapshotMapper.toJson(report, payload))
            );
            appendHistory(savedStatement, "GENERATED", EDeliveryWay.SYSTEM.name(), createdAt);
            appendLog(savedStatement, "GENERATED", "Reporte generado correctamente.", "description", "SUCCESS", createdAt);
        }

        return FinancialStatementGenerationResult.builder()
                .financialStatement(report)
                .financialStatementData(payload.getRows())
                .totalAssets(payload.getTotalAssets())
                .totalLiabilities(payload.getTotalLiabilities())
                .totalEquity(payload.getTotalEquity())
                .build();
    }

    private FinancialStatementEntity toEntity(FinancialStatementReport report, String snapshotJson) {
        return FinancialStatementEntity.builder()
                .reportId(report.getReportId())
                .type(report.getType())
                .entId(report.getEntId())
                .createdAt(report.getCreatedAt())
                .reportSnapshot(snapshotJson)
                .build();
    }

    private FinancialStatementReport toDomain(FinancialStatementEntity entity) {
        return FinancialStatementReport.builder()
                .reportId(entity.getReportId())
                .type(entity.getType())
                .entId(entity.getEntId())
                .criteria(extractCriteria(entity))
                .createdAt(entity.getCreatedAt())
                .downloadUrl(buildDownloadUrl(entity.getReportId()))
                .build();
    }

    private Pageable buildHistoryPageable(int page, int size, String sort) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size > 0 ? Math.min(size, 100) : 10;

        String sortProperty = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                sortProperty = parts[0].trim();
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, sortProperty));
    }

    private FinancialStatementHistoryItem toHistoryItem(FinancialStatementHistoryEntity entity) {
        FinancialStatementEntity statement = entity.getFinancialStatement();

        return FinancialStatementHistoryItem.builder()
                .reportId(statement.getReportId())
                .type(statement.getType())
                .entId(statement.getEntId())
                .criteria(extractCriteria(statement))
                .reportCreatedAt(statement.getCreatedAt())
                .state(entity.getState())
                .deliveryWay(entity.getDeliveryWay())
                .eventAt(entity.getCreatedAt())
                .downloadUrl(buildDownloadUrl(statement.getReportId()))
                .build();
    }

    private FinancialStatementLog toLog(FinancialStatementLogEntity entity) {
        return FinancialStatementLog.builder()
                .eventType(entity.getEventType())
                .message(entity.getMessage())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private FinancialStatementCriteria extractCriteria(FinancialStatementEntity entity) {
        var snapshot = financialStatementSnapshotMapper.fromJson(entity.getReportSnapshot());
        return snapshot != null ? snapshot.getCriteria() : null;
    }

    private FinancialStatementEntity resolveFinancialStatement(UUID reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("reportId is required.");
        }

        return financialStatementPersistencePort.findFinancialStatementByReportId(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Financial statement report not found."));
    }

    private void appendHistory(
            FinancialStatementEntity statement,
            String state,
            String deliveryWay,
            OffsetDateTime createdAt
    ) {
        financialStatementPersistencePort.saveHistory(
                FinancialStatementHistoryEntity.builder()
                        .financialStatement(statement)
                        .state(state)
                        .deliveryWay(deliveryWay)
                        .createdAt(createdAt)
                        .build()
        );
    }

    private void appendLog(
            FinancialStatementEntity statement,
            String eventType,
            String message,
            String icon,
            String color,
            OffsetDateTime createdAt
    ) {
        financialStatementPersistencePort.saveLog(
                FinancialStatementLogEntity.builder()
                        .financialStatement(statement)
                        .eventType(StringUtils.hasText(eventType) ? eventType.trim().toUpperCase() : "INFO")
                        .message(message)
                        .icon(icon)
                        .color(color)
                        .createdAt(createdAt)
                        .build()
        );
    }

    private String buildDownloadUrl(UUID reportId) {
        return "/api/financial-statements/" + reportId + "/download";
    }
}
