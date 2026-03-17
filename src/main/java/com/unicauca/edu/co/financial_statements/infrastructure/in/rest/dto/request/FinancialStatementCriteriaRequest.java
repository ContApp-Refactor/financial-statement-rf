package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementCriteriaRequest {
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
