package com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailSchedule;

import java.util.List;
import java.util.UUID;

public interface IFinancialStatementEmailSchedulePort {

    FinancialStatementEmailSchedule createSchedule(FinancialStatementEmailSchedule schedule);

    List<FinancialStatementEmailSchedule> getSchedulesByReportId(UUID reportId);

    FinancialStatementEmailSchedule updateScheduleStatus(Long scheduleId, boolean active);

    void processDueSchedules();
}
