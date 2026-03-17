package com.unicauca.edu.co.financial_statements.domain.models.core;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialStatementHistoryItem {
    private UUID reportId;
    private EFinancialStatementType type;
    private String entId;
    private FinancialStatementCriteria criteria;
    private OffsetDateTime reportCreatedAt;
    private String state;
    private String deliveryWay;
    private OffsetDateTime eventAt;
    private String downloadUrl;
}
