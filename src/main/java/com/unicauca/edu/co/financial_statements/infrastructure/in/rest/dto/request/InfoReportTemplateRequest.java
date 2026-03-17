package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InfoReportTemplateRequest {
    private Integer id;
    private String name;
    private String pathLogotype;
    private String alienation;
    private String font;
    private Integer fontSize;
    private String mainColor;
}
