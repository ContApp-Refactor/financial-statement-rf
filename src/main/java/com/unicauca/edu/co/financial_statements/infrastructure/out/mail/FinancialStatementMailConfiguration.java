package com.unicauca.edu.co.financial_statements.infrastructure.out.mail;

import com.resend.Resend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class FinancialStatementMailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mail", name = "provider", havingValue = "resend")
    Resend resendClient(MailProperties mailProperties) {
        String apiKey = mailProperties.getResend().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Resend is selected but mail.resend.api-key is empty.");
        }

        return new Resend(apiKey.trim());
    }
}
