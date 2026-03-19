package com.unicauca.edu.co.financial_statements.domain.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementVisualSignature {
    private String fileName;
    private String contentType;
    private byte[] content;
    private String signerName;
    private String signerRole;
}
