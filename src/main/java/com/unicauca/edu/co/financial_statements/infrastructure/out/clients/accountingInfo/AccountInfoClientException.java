package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo;

import org.springframework.http.HttpStatus;

public class AccountInfoClientException extends RuntimeException {

    private final HttpStatus status;

    public AccountInfoClientException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AccountInfoClientException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
