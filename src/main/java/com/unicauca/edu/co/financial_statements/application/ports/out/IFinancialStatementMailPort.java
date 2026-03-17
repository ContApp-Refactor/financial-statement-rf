package com.unicauca.edu.co.financial_statements.application.ports.out;

public interface IFinancialStatementMailPort {

    void sendReport(
            String toEmail,
            String subject,
            String body,
            byte[] fileContent,
            String fileName,
            String contentType
    ) throws Exception;
}
