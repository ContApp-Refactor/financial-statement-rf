package com.unicauca.edu.co.financial_statements.domain.models.core;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementSnapshot {

    public static final int CURRENT_VERSION = 1;

    private Integer version;
    private UUID reportId;
    private EFinancialStatementType type;
    private String entId;
    private FinancialStatementCriteria criteria;
    private OffsetDateTime createdAt;
    private List<FinancialStatementRow> financialStatementData;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
}
