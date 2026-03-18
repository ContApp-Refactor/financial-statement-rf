package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VisualSignatureRequest {
    private String fileName;
    private String contentType;
    private String base64Content;
}
