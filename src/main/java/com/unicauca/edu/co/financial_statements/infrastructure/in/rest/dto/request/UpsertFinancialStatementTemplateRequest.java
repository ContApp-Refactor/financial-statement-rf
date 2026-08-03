package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpsertFinancialStatementTemplateRequest {

    private Long id;

    @NotBlank(message = "El enterpriseId es obligatorio.")
    private String enterpriseId;

    @NotBlank(message = "El nombre de la plantilla es obligatorio.")
    private String name;

    private String pathLogotype;

    private String alignment;

    private String font;

    private Integer fontSize;

    private String mainColor;

    private Boolean isDefault;
}
