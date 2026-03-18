package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.mapper;

import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementReportMetadataMapper;
import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementRowMapper;
import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementTemplateExportStyleMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.ExportFinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.InfoReportTemplateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementRestMapperTest {

    private final FinancialStatementRestMapper mapper =
            new FinancialStatementRestMapper(
                    new FinancialStatementRowMapper(),
                    new FinancialStatementReportMetadataMapper(),
                    new FinancialStatementTemplateExportStyleMapper()
            );

    @Test
    void shouldConvertRequestRowsToTypedRows() {
        ExportFinancialStatementRequest request = ExportFinancialStatementRequest.builder()
                .reportId(UUID.randomUUID())
                .format("PDF")
                .entName("ENT-001")
                .financialStatement(Map.of("type", "INCOME_STATEMENT"))
                .financialStatementData(List.of(Map.of(
                        "lineDescription", "Ingresos ordinarios",
                        "currentAmount", new BigDecimal("175000000.00"),
                        "yearValues", Map.of("2025", new BigDecimal("12.00")),
                        "account", Map.of(
                                "accountCode", "413505",
                                "accountDescription", "Ventas nacionales",
                                "nature", "CREDITO"
                        )
                )))
                .build();

        var command = mapper.toDomain(request);

        assertThat(command).isNotNull();
        assertThat(command.getFormat()).isEqualTo(EReportExportFormat.PDF);
        assertThat(command.getFinancialStatementData()).hasSize(1);
        FinancialStatementRow row = command.getFinancialStatementData().get(0);
        assertThat(row.getLineDescription()).isEqualTo("Ingresos ordinarios");
        assertThat(row.getCurrentAmount()).isEqualByComparingTo("175000000.00");
        assertThat(row.getYearValues()).containsEntry("2025", new BigDecimal("12.00"));
        assertThat(row.getAccount()).isNotNull();
        assertThat(row.getAccount().getAccountCode()).isEqualTo("413505");
        assertThat(row.getAccount().getNature()).isEqualTo("CREDITO");
    }

    @Test
    void shouldKeepTypedRowsWhenBuildingPreviewExportCommand() {
        UUID reportId = UUID.randomUUID();
        FinancialStatementRow previewRow = FinancialStatementRow.builder()
                .lineDescription("ACTIVO")
                .currentAmount(new BigDecimal("724000000.00"))
                .build();

        FinancialStatementGenerationResult preview = FinancialStatementGenerationResult.builder()
                .financialStatement(FinancialStatementReport.builder()
                        .reportId(reportId)
                        .type(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION)
                        .entId("ENT-002")
                        .createdAt(OffsetDateTime.parse("2026-03-16T23:00:00-05:00"))
                        .build())
                .financialStatementData(List.of(previewRow))
                .build();

        var command = mapper.toPreviewExportCommand(preview, "EXCEL");

        assertThat(command).isNotNull();
        assertThat(command.getFormat()).isEqualTo(EReportExportFormat.EXCEL);
        assertThat(command.getEnterpriseName()).isEqualTo("ENT-002");
        assertThat(command.getFinancialStatementData()).containsExactly(previewRow);
    }

    @Test
    void shouldMapExportStyleUsingAlignmentFieldFromTemplateRequest() {
        ExportFinancialStatementRequest request = ExportFinancialStatementRequest.builder()
                .format("PDF")
                .infoReportTemplate(InfoReportTemplateRequest.builder()
                        .alignment("center")
                        .font("Helvetica")
                        .fontSize(12)
                        .mainColor("#003366")
                        .pathLogotype("https://contapp/logo.png")
                        .build())
                .build();

        var command = mapper.toDomain(request);

        assertThat(command).isNotNull();
        assertThat(command.getExportStyle()).isNotNull();
        assertThat(command.getExportStyle().getAlignment()).isEqualTo("CENTER");
        assertThat(command.getExportStyle().getFont()).isEqualTo("Helvetica");
        assertThat(command.getExportStyle().getFontSize()).isEqualTo(12);
        assertThat(command.getExportStyle().getMainColor()).isEqualTo("#003366");
        assertThat(command.getExportStyle().getPathLogotype()).isEqualTo("https://contapp/logo.png");
    }
}
