package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementExportPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementMailPort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAnnotation;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementRow;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementVisualSignature;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EDeliveryWay;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.same;

@ExtendWith(MockitoExtension.class)
class FinancialStatementDeliveryUCTest {

    @Mock
    private IFinancialStatementCommandPort financialStatementCommandPort;

    @Mock
    private IFinancialStatementExportPort financialStatementExportPort;

    @Mock
    private IFinancialStatementMailPort financialStatementMailPort;

    private FinancialStatementDeliveryUC useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinancialStatementDeliveryUC(
                financialStatementCommandPort,
                financialStatementExportPort,
                financialStatementMailPort,
                new FinancialStatementReportNameResolver(),
                new FinancialStatementRowMapper(),
                new FinancialStatementReportMetadataMapper(),
                new FinancialStatementTemplateExportStyleMapper()
        );
    }

    @Test
    void shouldConvertTypedRowsToMapsOnlyAtExportBoundary() {
        UUID reportId = UUID.randomUUID();
        FinancialStatementRow row = FinancialStatementRow.builder()
                .lineDescription("Ingresos ordinarios")
                .currentAmount(new BigDecimal("175000000.00"))
                .yearValues(Map.of("2025", new BigDecimal("12.00")))
                .build();

        when(financialStatementCommandPort.getDefaultTemplateByEnterprise("ENT-001"))
                .thenReturn(Optional.empty());
        when(financialStatementExportPort.export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Resultados"),
                eq("ENT-001"),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(new IFinancialStatementExportPort.ExportedDocument(
                "pdf".getBytes(),
                "application/pdf",
                "report.pdf"
        ));

        useCase.export(FinancialStatementExportCommand.builder()
                .reportId(reportId)
                .format(EReportExportFormat.PDF)
                .enterpriseName("ENT-001")
                .financialStatement(Map.of(
                        "type", "INCOME_STATEMENT",
                        "criteria", Map.of("startDate", "2025-01-01")
                ))
                .financialStatementData(List.of(row))
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(financialStatementExportPort).export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Resultados"),
                eq("ENT-001"),
                rowsCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );
        verify(financialStatementCommandPort).registerDeliveryEvent(
                eq(reportId),
                eq(EDeliveryWay.DOWNLOAD.name()),
                eq("Reporte exportado en formato PDF."),
                eq("EXPORTED")
        );

        List<Map<String, Object>> exportedRows = rowsCaptor.getValue();
        assertThat(exportedRows).hasSize(1);
        assertThat(exportedRows.get(0)).containsEntry("lineDescription", "Ingresos ordinarios");
        assertThat(exportedRows.get(0)).containsKey("yearValues");
    }

    @Test
    void shouldUseTypedSnapshotRowsWhenCommandDoesNotProvideRows() {
        UUID reportId = UUID.randomUUID();
        FinancialStatementRow snapshotRow = FinancialStatementRow.builder()
                .lineDescription("Ganancias acumuladas")
                .yearValues(Map.of("2025", new BigDecimal("40.00")))
                .build();

        FinancialStatementGenerationResult snapshot = FinancialStatementGenerationResult.builder()
                .financialStatement(FinancialStatementReport.builder()
                        .reportId(reportId)
                        .type(EFinancialStatementType.STATEMENT_CHANGES_EQUITY)
                        .entId("ENT-002")
                        .criteria(FinancialStatementCriteria.builder()
                                .startDate(LocalDate.of(2025, 1, 1))
                                .endDate(LocalDate.of(2025, 12, 31))
                                .build())
                        .createdAt(OffsetDateTime.parse("2026-03-16T23:00:00-05:00"))
                        .build())
                .financialStatementData(List.of(snapshotRow))
                .build();

        when(financialStatementCommandPort.getFinancialStatementSnapshot(reportId))
                .thenReturn(Optional.of(snapshot));
        when(financialStatementCommandPort.getDefaultTemplateByEnterprise("ENT-002"))
                .thenReturn(Optional.empty());
        when(financialStatementExportPort.export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Cambios en el Patrimonio"),
                eq("ENT-002"),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(new IFinancialStatementExportPort.ExportedDocument(
                "pdf".getBytes(),
                "application/pdf",
                "equity.pdf"
        ));

        useCase.export(FinancialStatementExportCommand.builder()
                .reportId(reportId)
                .format(EReportExportFormat.PDF)
                .financialStatement(Map.of("type", "STATEMENT_CHANGES_EQUITY"))
                .financialStatementData(List.of())
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(financialStatementExportPort).export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Cambios en el Patrimonio"),
                eq("ENT-002"),
                rowsCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );

        assertThat(rowsCaptor.getValue()).hasSize(1);
        assertThat(rowsCaptor.getValue().get(0)).containsEntry("lineDescription", "Ganancias acumuladas");
    }

    @Test
    void shouldKeepSelectedTemplateAnnotationsAndSignatureWhenExportingByEmail() throws Exception {
        UUID reportId = UUID.randomUUID();
        FinancialStatementRow snapshotRow = FinancialStatementRow.builder()
                .lineDescription("Caja")
                .currentAmount(new BigDecimal("1000.00"))
                .build();
        FinancialStatementGenerationResult snapshot = FinancialStatementGenerationResult.builder()
                .financialStatement(FinancialStatementReport.builder()
                        .reportId(reportId)
                        .type(EFinancialStatementType.STATEMENT_FINANCIAL_POSITION)
                        .entId("ENT-EMAIL-001")
                        .criteria(FinancialStatementCriteria.builder()
                                .currentCutoffDate(LocalDate.of(2026, 12, 31))
                                .previousCutoffDate(LocalDate.of(2025, 12, 31))
                                .build())
                        .createdAt(OffsetDateTime.parse("2026-03-18T10:00:00-05:00"))
                        .build())
                .financialStatementData(List.of(snapshotRow))
                .build();
        FinancialStatementExportStyle exportStyle = FinancialStatementExportStyle.builder()
                .pathLogotype("https://contapp/logo.png")
                .alignment("CENTER")
                .font("Helvetica")
                .fontSize(12)
                .mainColor("#003366")
                .build();
        FinancialStatementVisualSignature visualSignature = FinancialStatementVisualSignature.builder()
                .fileName("firma.png")
                .contentType("image/png")
                .content(new byte[]{1, 2, 3})
                .build();

        when(financialStatementCommandPort.getFinancialStatementSnapshot(reportId))
                .thenReturn(Optional.of(snapshot));
        when(financialStatementExportPort.export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Situacion Financiera"),
                eq("ENT-EMAIL-001"),
                any(),
                any(),
                same(exportStyle),
                eq(List.of("Observacion para correo")),
                same(visualSignature)
        )).thenReturn(new IFinancialStatementExportPort.ExportedDocument(
                "pdf".getBytes(),
                "application/pdf",
                "reporte.pdf"
        ));

        useCase.exportByEmail(FinancialStatementEmailExportCommand.builder()
                .reportId(reportId)
                .format(EReportExportFormat.PDF)
                .toEmail("usuario@dominio.com")
                .annotations(List.of(FinancialStatementAnnotation.builder()
                        .text("Observacion para correo")
                        .build()))
                .visualSignature(visualSignature)
                .exportStyle(exportStyle)
                .build());

        verify(financialStatementExportPort).export(
                eq(EReportExportFormat.PDF),
                eq("Estado de Situacion Financiera"),
                eq("ENT-EMAIL-001"),
                any(),
                any(),
                same(exportStyle),
                eq(List.of("Observacion para correo")),
                same(visualSignature)
        );
        verify(financialStatementMailPort).sendReport(
                eq("usuario@dominio.com"),
                eq("Financial Statement Export"),
                eq("Adjunto encontraras el reporte financiero solicitado."),
                any(),
                eq("reporte.pdf"),
                eq("application/pdf")
        );
        verify(financialStatementCommandPort).registerDeliveryEvent(
                eq(reportId),
                eq(EDeliveryWay.EMAIL.name()),
                eq("Reporte enviado por correo a usuario@dominio.com."),
                eq("EMAILED")
        );
    }
}
