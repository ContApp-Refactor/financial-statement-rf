package com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountInfoMovementResponse {
    private String entId;
    private String date;
    private VoucherResponse voucher;
    private AccountResponse account;
    private Long thirdPartyId;
    private AccountingMovementResponse accountingMovement;
    private CostCenterResponse costCenter;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VoucherResponse {
        private Integer number;
        private String type;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountResponse {
        private String code;
        private String nature;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountingMovementResponse {
        private String description;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CostCenterResponse {
        private Long code;
        private String name;
    }
}
