package com.unicauca.edu.co.financial_statements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FinancialStatementsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialStatementsApplication.class, args);
    }
}
