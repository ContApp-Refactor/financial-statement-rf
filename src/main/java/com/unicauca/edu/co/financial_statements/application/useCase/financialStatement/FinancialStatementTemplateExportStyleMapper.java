package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementTemplateExportStyleMapper {

    public FinancialStatementExportStyle toExportStyle(FinancialStatementTemplate template) {
        if (template == null) {
            return null;
        }

        return FinancialStatementExportStyle.builder()
                .pathLogotype(template.getPathLogotype())
                .alignment(template.getAlignment())
                .font(template.getFont())
                .fontSize(template.getFontSize())
                .mainColor(template.getMainColor())
                .build();
    }
}
