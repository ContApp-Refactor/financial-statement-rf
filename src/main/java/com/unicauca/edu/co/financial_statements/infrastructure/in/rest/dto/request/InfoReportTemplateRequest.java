package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("alienation")
    private String alignment;
    private String font;
    private Integer fontSize;
    private String mainColor;
}
