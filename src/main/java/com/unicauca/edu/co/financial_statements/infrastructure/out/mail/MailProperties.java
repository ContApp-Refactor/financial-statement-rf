package com.unicauca.edu.co.financial_statements.infrastructure.out.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String provider = "smtp";
    private String from;
    private final ResendProperties resend = new ResendProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public ResendProperties getResend() {
        return resend;
    }

    public static class ResendProperties {
        private String apiKey;
        private String replyTo;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }
    }
}
