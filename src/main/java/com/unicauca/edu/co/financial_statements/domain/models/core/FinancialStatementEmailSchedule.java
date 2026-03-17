package com.unicauca.edu.co.financial_statements.domain.models.core;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportDeliveryFrequency;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementEmailSchedule {
    private Long id;
    private UUID reportId;
    private String recipientEmail;
    private EReportExportFormat format;
    private EReportDeliveryFrequency frequency;
    private Integer hourOfDay;
    private Integer minuteOfHour;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private String timezone;
    private Boolean active;
    private OffsetDateTime nextRunAt;
    private OffsetDateTime lastRunAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
