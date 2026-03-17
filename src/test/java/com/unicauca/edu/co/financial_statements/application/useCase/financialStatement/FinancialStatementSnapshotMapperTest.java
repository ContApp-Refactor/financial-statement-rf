package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDataPayload;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementSnapshot;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementSnapshotMapperTest {

    private final FinancialStatementSnapshotMapper mapper =
            new FinancialStatementSnapshotMapper(
                    new ObjectMapper().findAndRegisterModules(),
                    new FinancialStatementRowMapper()
            );

    @Test
    void shouldSerializeVersionedSnapshot() {
        UUID reportId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-16T21:00:00-05:00");

        String json = mapper.toJson(
                FinancialStatementReport.builder()
                        .reportId(reportId)
                        .type(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION)
                        .entId("ENT-001")
                        .criteria(FinancialStatementCriteria.builder()
                                .currentCutoffDate(LocalDate.of(2025, 3, 29))
                                .previousCutoffDate(LocalDate.of(2024, 3, 29))
                                .build())
                        .createdAt(createdAt)
                        .build(),
                FinancialStatementDataPayload.builder()
                        .rows(List.of(FinancialStatementRow.builder().lineDescription("ACTIVO").build()))
                        .totalAssets(new BigDecimal("100.00"))
                        .totalLiabilities(new BigDecimal("40.00"))
                        .totalEquity(new BigDecimal("60.00"))
                        .build()
        );

        FinancialStatementSnapshot snapshot = mapper.fromJson(json);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getVersion()).isEqualTo(FinancialStatementSnapshot.CURRENT_VERSION);
        assertThat(snapshot.getReportId()).isEqualTo(reportId);
        assertThat(snapshot.getFinancialStatementData()).hasSize(1);
        assertThat(snapshot.getFinancialStatementData().get(0).getLineDescription()).isEqualTo("ACTIVO");
        assertThat(snapshot.getTotalAssets()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReadLegacySnapshotWithoutVersion() {
        UUID reportId = UUID.randomUUID();
        String legacyJson = """
                {
                  "reportId": "%s",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "entId": "ENT-LEGACY",
                  "criteria": {
                    "currentCutoffDate": "2025-03-29",
                    "previousCutoffDate": "2024-03-29"
                  },
                  "createdAt": "2026-03-16T21:00:00-05:00",
                  "financialStatementData": [
                    {
                      "lineDescription": "ACTIVO",
                      "rowType": "SECTION"
                    }
                  ],
                  "totalAssets": 100,
                  "totalLiabilities": 40,
                  "totalEquity": 60
                }
                """.formatted(reportId);

        FinancialStatementEntity entity = FinancialStatementEntity.builder()
                .reportId(reportId)
                .type(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION)
                .entId("ENT-LEGACY")
                .createdAt(OffsetDateTime.parse("2026-03-16T21:00:00-05:00"))
                .reportSnapshot(legacyJson)
                .build();

        var result = mapper.toGenerationResult(entity);

        assertThat(result.getFinancialStatement()).isNotNull();
        assertThat(result.getFinancialStatement().getCriteria()).isNotNull();
        assertThat(result.getFinancialStatement().getCriteria().getCurrentCutoffDate())
                .isEqualTo(LocalDate.of(2025, 3, 29));
        assertThat(result.getFinancialStatementData()).hasSize(1);
        assertThat(result.getFinancialStatementData().get(0).getLineDescription()).isEqualTo("ACTIVO");
        assertThat(result.getTotalAssets()).isEqualByComparingTo("100");
    }
}
