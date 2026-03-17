package com.unicauca.edu.co.financial_statements.infrastructure.out.mail;

import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementMailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialStatementMailAdapter implements IFinancialStatementMailPort {

    private final FinancialStatementMailService financialStatementMailService;

    @Override
    public void sendReport(
            String toEmail,
            String subject,
            String body,
            byte[] fileContent,
            String fileName,
            String contentType
    ) throws Exception {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        financialStatementMailService.sendReport(
                toEmail,
                subject,
                body,
                fileContent,
                fileName,
                mediaType
        );
    }
}
