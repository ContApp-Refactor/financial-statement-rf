package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.account-info-service")
public class AccountInfoClientProperties {

    private String baseUrl;
    private String bearerToken;
    private boolean mockModeEnabled;
    private boolean acceptLegacyDateFormat;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public boolean isMockModeEnabled() {
        return mockModeEnabled;
    }

    public void setMockModeEnabled(boolean mockModeEnabled) {
        this.mockModeEnabled = mockModeEnabled;
    }

    public boolean isAcceptLegacyDateFormat() {
        return acceptLegacyDateFormat;
    }

    public void setAcceptLegacyDateFormat(boolean acceptLegacyDateFormat) {
        this.acceptLegacyDateFormat = acceptLegacyDateFormat;
    }
}
