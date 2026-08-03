package com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDocument;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;

import java.util.UUID;

public interface IFinancialStatementDeliveryPort {

    FinancialStatementDocument export(FinancialStatementExportCommand command);

    FinancialStatementDocument download(UUID reportId, EReportExportFormat format);
}
