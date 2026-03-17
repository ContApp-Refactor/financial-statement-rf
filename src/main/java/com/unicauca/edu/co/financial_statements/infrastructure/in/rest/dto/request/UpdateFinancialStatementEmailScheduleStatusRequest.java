package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateFinancialStatementEmailScheduleStatusRequest {

    @NotNull(message = "active is required")
    private Boolean active;
}
