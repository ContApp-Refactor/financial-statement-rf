package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportDeliveryFrequency;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateFinancialStatementEmailScheduleRequest {

    @NotNull(message = "reportId is required")
    private UUID reportId;

    @NotBlank(message = "recipientEmail is required")
    @Email(message = "recipientEmail must be a valid email")
    private String recipientEmail;

    private EReportExportFormat format;

    @NotNull(message = "frequency is required")
    private EReportDeliveryFrequency frequency;

    @NotNull(message = "hourOfDay is required")
    @Min(value = 0, message = "hourOfDay must be between 0 and 23")
    @Max(value = 23, message = "hourOfDay must be between 0 and 23")
    private Integer hourOfDay;

    @NotNull(message = "minuteOfHour is required")
    @Min(value = 0, message = "minuteOfHour must be between 0 and 59")
    @Max(value = 59, message = "minuteOfHour must be between 0 and 59")
    private Integer minuteOfHour;

    @Min(value = 1, message = "dayOfWeek must be between 1 and 7")
    @Max(value = 7, message = "dayOfWeek must be between 1 and 7")
    private Integer dayOfWeek;

    @Min(value = 1, message = "dayOfMonth must be between 1 and 31")
    @Max(value = 31, message = "dayOfMonth must be between 1 and 31")
    private Integer dayOfMonth;

    private String timezone;
}
