package com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountingEntry {
    private String entId;
    private LocalDate date;
    private String accountCode;
    private String accountName;
    private String accountNature;
    private Integer voucherNumber;
    private String voucherType;
    private Long thirdPartyId;
    private Long costCenterCode;
    private BigDecimal debit;
    private BigDecimal credit;
    private String movementDescription;
}
