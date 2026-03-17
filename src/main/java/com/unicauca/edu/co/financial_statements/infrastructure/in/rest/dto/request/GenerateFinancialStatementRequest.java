package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenerateFinancialStatementRequest {

    @NotBlank(message = "entId is required")
    private String entId;

    @NotNull(message = "type is required")
    private EFinancialStatementType type;

    @Valid
    private FinancialStatementCriteriaRequest criteria;

    private String criteriaType;
    private FinancialStatementCriteriaRangeRequest criteriaRange;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate previousStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate previousEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate currentCutoffDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
    private LocalDate previousCutoffDate;
}
