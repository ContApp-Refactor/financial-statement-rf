package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementPersistencePort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FinancialStatementTemplateManager {

    private static final int MAX_TEMPLATES_PER_ENTERPRISE = 3;

    private final IFinancialStatementPersistencePort financialStatementPersistencePort;

    public FinancialStatementTemplate saveTemplate(FinancialStatementTemplate template) {
        validateTemplate(template);

        String enterpriseId = template.getEntId().trim();
        FinancialStatementTemplateEntity entity = resolveTemplateEntity(template, enterpriseId);

        if (entity.getId() == null
                && financialStatementPersistencePort.countTemplatesByEnterprise(enterpriseId) >= MAX_TEMPLATES_PER_ENTERPRISE) {
            throw new IllegalArgumentException("A maximum of 3 templates is allowed per enterprise.");
        }

        boolean shouldBeDefault = resolveDefaultFlag(template, entity, enterpriseId);
        if (shouldBeDefault) {
            clearDefaultTemplateFlags(enterpriseId, entity.getId());
        }

        entity.setEntId(enterpriseId);
        entity.setName(template.getName().trim());
        entity.setPathLogotype(template.getPathLogotype());
        entity.setAlignment(normalizeAlignment(template.getAlignment()));
        entity.setFont(template.getFont());
        entity.setFontSize(template.getFontSize());
        entity.setMainColor(template.getMainColor());
        entity.setIsDefault(shouldBeDefault);

        return toTemplate(financialStatementPersistencePort.saveTemplate(entity));
    }

    public FinancialStatementTemplate saveDefaultTemplate(FinancialStatementTemplate template) {
        FinancialStatementTemplate templateToSave = template != null
                ? FinancialStatementTemplate.builder()
                .id(template.getId())
                .entId(template.getEntId())
                .name(template.getName())
                .pathLogotype(template.getPathLogotype())
                .alignment(template.getAlignment())
                .font(template.getFont())
                .fontSize(template.getFontSize())
                .mainColor(template.getMainColor())
                .isDefault(Boolean.TRUE)
                .build()
                : null;

        return saveTemplate(templateToSave);
    }

    public List<FinancialStatementTemplate> getTemplatesByEnterprise(String enterpriseId) {
        if (!StringUtils.hasText(enterpriseId)) {
            throw new IllegalArgumentException("enterpriseId is required.");
        }

        return financialStatementPersistencePort.findTemplatesByEnterprise(enterpriseId.trim()).stream()
                .map(this::toTemplate)
                .toList();
    }

    public Optional<FinancialStatementTemplate> getDefaultTemplateByEnterprise(String enterpriseId) {
        if (!StringUtils.hasText(enterpriseId)) {
            throw new IllegalArgumentException("enterpriseId is required.");
        }

        return financialStatementPersistencePort.findDefaultTemplateByEnterprise(enterpriseId.trim())
                .map(this::toTemplate);
    }

    private void validateTemplate(FinancialStatementTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template is required.");
        }
        if (!StringUtils.hasText(template.getEntId())) {
            throw new IllegalArgumentException("enterpriseId is required.");
        }
        if (!StringUtils.hasText(template.getName())) {
            throw new IllegalArgumentException("name is required.");
        }
    }

    private String normalizeAlignment(String alignment) {
        return StringUtils.hasText(alignment) ? alignment.trim().toUpperCase() : null;
    }

    private FinancialStatementTemplateEntity resolveTemplateEntity(
            FinancialStatementTemplate template,
            String enterpriseId
    ) {
        if (template.getId() == null) {
            return FinancialStatementTemplateEntity.builder()
                    .createdAt(OffsetDateTime.now())
                    .build();
        }

        return financialStatementPersistencePort.findTemplateByIdAndEnterprise(template.getId(), enterpriseId)
                .orElseThrow(() -> new IllegalArgumentException("Financial statement template not found."));
    }

    private boolean resolveDefaultFlag(
            FinancialStatementTemplate template,
            FinancialStatementTemplateEntity currentEntity,
            String enterpriseId
    ) {
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            return true;
        }

        if (template.getIsDefault() == null) {
            if (Boolean.TRUE.equals(currentEntity.getIsDefault())) {
                return true;
            }

            return financialStatementPersistencePort.findDefaultTemplateByEnterprise(enterpriseId).isEmpty();
        }

        return false;
    }

    private void clearDefaultTemplateFlags(String enterpriseId, Long excludedTemplateId) {
        financialStatementPersistencePort.findTemplatesByEnterprise(enterpriseId).stream()
                .filter(template -> Boolean.TRUE.equals(template.getIsDefault()))
                .filter(template -> !Objects.equals(template.getId(), excludedTemplateId))
                .forEach(template -> {
                    template.setIsDefault(Boolean.FALSE);
                    financialStatementPersistencePort.saveTemplate(template);
                });
    }

    private FinancialStatementTemplate toTemplate(FinancialStatementTemplateEntity entity) {
        return FinancialStatementTemplate.builder()
                .id(entity.getId())
                .entId(entity.getEntId())
                .name(entity.getName())
                .pathLogotype(entity.getPathLogotype())
                .alignment(entity.getAlignment())
                .font(entity.getFont())
                .fontSize(entity.getFontSize())
                .mainColor(entity.getMainColor())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
