package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteFinancialStatementTemplatesRequest {
    @NotBlank(message = "El enterpriseId es obligatorio.")
    private String enterpriseId;

    @NotEmpty(message = "Los templateIds son obligatorios.")
    private List<Long> templateIds;
}
