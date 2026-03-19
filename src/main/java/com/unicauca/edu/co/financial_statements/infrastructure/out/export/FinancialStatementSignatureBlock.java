package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

public record FinancialStatementSignatureBlock(
        byte[] image,
        String signerName,
        String signerRole
) {
}
