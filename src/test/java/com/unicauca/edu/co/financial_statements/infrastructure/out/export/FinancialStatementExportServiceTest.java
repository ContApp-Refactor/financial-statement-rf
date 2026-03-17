package com.unicauca.edu.co.financial_statements.infrastructure.out.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper.FinancialStatementJasperDesignFactory;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper.FinancialStatementJasperRenderer;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper.FinancialStatementJasperStyleResolver;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementExportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FinancialStatementExportCriteriaResolver criteriaResolver =
            new FinancialStatementExportCriteriaResolver(objectMapper);
    private final FinancialStatementEquityMatrixBuilder equityMatrixBuilder =
            new FinancialStatementEquityMatrixBuilder(criteriaResolver);
    private final FinancialStatementPositionTableBuilder positionTableBuilder =
            new FinancialStatementPositionTableBuilder();
    private final FinancialStatementTabularDataResolver tabularDataResolver =
            new FinancialStatementTabularDataResolver(objectMapper);
    private final DefaultFinancialStatementTableModelResolver tableModelResolver =
            new DefaultFinancialStatementTableModelResolver(
                    criteriaResolver,
                    equityMatrixBuilder,
                    positionTableBuilder,
                    tabularDataResolver
            );
    private final FinancialStatementJasperStyleResolver styleResolver =
            new FinancialStatementJasperStyleResolver();
    private final FinancialStatementJasperDesignFactory jasperDesignFactory =
            new FinancialStatementJasperDesignFactory(
                    new DefaultResourceLoader(),
                    styleResolver
            );
    private final FinancialStatementJasperRenderer jasperRenderer =
            new FinancialStatementJasperRenderer(jasperDesignFactory, styleResolver);
    private final FinancialStatementExportService exportService =
            new FinancialStatementExportService(
                    new FinancialStatementExportRowMapper(),
                    tableModelResolver,
                    jasperRenderer
            );

    @Test
    void shouldPreserveFrontendDescriptionsAndCriteriaInFinancialPositionPdf() throws IOException {
        List<Map<String, Object>> rows = List.of(
                financialPositionRow("ACTIVO", "SECTION", "0", "0", "0", "0", "0", "0"),
                financialPositionRow("ACTIVO CORRIENTE", "SUBSECTION", "0", "0", "0", "0", "0", "0"),
                financialPositionRow(
                        "Efectivo y equivalente al efectivo",
                        "DETAIL",
                        "270000000",
                        "38.35",
                        "120000000",
                        "38.59",
                        "150000000",
                        "-0.24"
                ),
                financialPositionRow(
                        "1 - Activo corriente - Caja",
                        "DETAIL",
                        "270000000",
                        "38.35",
                        "120000000",
                        "38.59",
                        "150000000",
                        "-0.24"
                )
        );

        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "STATEMENT_FINANCIAL_POSITION");
        financialStatement.put("criteria", Map.of(
                "criteriaType", "GROUP",
                "currentCutoffDate", "2025-03-29",
                "previousCutoffDate", "2024-03-29"
        ));

        FinancialStatementExportService.ExportedFile exportedFile = exportService.export(
                EReportExportFormat.PDF,
                "Estado de Situacion Financiera",
                "PEPSI",
                rows,
                financialStatement,
                null
        );

        try (PDDocument document = Loader.loadPDF(exportedFile.content())) {
            String pdfText = new PDFTextStripper().getText(document);

            assertThat(pdfText)
                    .contains("Tipo de Nivel: Grupo")
                    .contains("Efectivo y equivalente al efectivo")
                    .contains("1 - Activo corriente - Caja");
        }
    }

    @Test
    void shouldIncludeIncomeStatementCriteriaInPdfAndExcel() throws IOException {
        List<Map<String, Object>> rows = List.of(
                financialPositionRow("Ingresos ordinarios", "DETAIL", "175000000", "100", "156000000", "100", "19000000", "0"),
                financialPositionRow("INGRESOS NETOS OPERACIONALES", "TOTAL", "163000000", "93.14", "146000000", "93.59", "17000000", "-0.45")
        );

        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "INCOME_STATEMENT");
        financialStatement.put("criteria", Map.of(
                "criteriaType", "SUB_ACCOUNT",
                "startDate", "2025-01-01",
                "endDate", "2025-03-29",
                "previousStartDate", "2024-01-01",
                "previousEndDate", "2024-03-29"
        ));

        FinancialStatementExportService.ExportedFile pdfFile = exportService.export(
                EReportExportFormat.PDF,
                "Estado de Resultados",
                "ENT-TEST-001",
                rows,
                financialStatement,
                null
        );

        try (PDDocument document = Loader.loadPDF(pdfFile.content())) {
            String pdfText = new PDFTextStripper().getText(document);

            assertThat(pdfText)
                    .contains("Tipo de Nivel: Subcuenta")
                    .contains("Fecha de Inicio Periodo Actual: 01/01/2025")
                    .contains("Fecha de Fin Periodo Actual: 29/03/2025")
                    .contains("Fecha de Inicio Periodo Anterior: 01/01/2024")
                    .contains("Fecha de Fin Periodo Anterior: 29/03/2024");
        }

        FinancialStatementExportService.ExportedFile excelFile = exportService.export(
                EReportExportFormat.EXCEL,
                "Estado de Resultados",
                "ENT-TEST-001",
                rows,
                financialStatement,
                null
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelFile.content()))) {
            String sheetText = extractSheetText(workbook);

            assertThat(sheetText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel")
                    .contains("Subcuenta")
                    .contains("Fecha de Inicio Periodo Actual")
                    .contains("01/01/2025")
                    .contains("Fecha de Fin Periodo Actual")
                    .contains("29/03/2025")
                    .contains("Fecha de Inicio Periodo Anterior")
                    .contains("01/01/2024")
                    .contains("Fecha de Fin Periodo Anterior")
                    .contains("29/03/2024");
        }
    }

    @Test
    void shouldIncludeCriteriaWhenFinancialStatementMetadataUsesDomainObjects() throws IOException {
        List<Map<String, Object>> rows = List.of(
                financialPositionRow("Ingresos ordinarios", "DETAIL", "175000000", "100", "156000000", "100", "19000000", "0"),
                financialPositionRow("INGRESOS NETOS OPERACIONALES", "TOTAL", "163000000", "93.14", "146000000", "93.59", "17000000", "-0.45")
        );

        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "INCOME_STATEMENT");
        financialStatement.put("criteria", FinancialStatementCriteria.builder()
                .criteriaType("SUB_ACCOUNT")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 3, 29))
                .previousStartDate(LocalDate.of(2024, 1, 1))
                .previousEndDate(LocalDate.of(2024, 3, 29))
                .build());

        FinancialStatementExportService.ExportedFile pdfFile = exportService.export(
                EReportExportFormat.PDF,
                "Estado de Resultados",
                "ENT-TEST-001",
                rows,
                financialStatement,
                null
        );

        try (PDDocument document = Loader.loadPDF(pdfFile.content())) {
            String pdfText = new PDFTextStripper().getText(document);

            assertThat(pdfText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel: Subcuenta")
                    .contains("Fecha de Inicio Periodo Actual: 01/01/2025")
                    .contains("Fecha de Fin Periodo Actual: 29/03/2025")
                    .contains("Fecha de Inicio Periodo Anterior: 01/01/2024")
                    .contains("Fecha de Fin Periodo Anterior: 29/03/2024");
        }

        FinancialStatementExportService.ExportedFile excelFile = exportService.export(
                EReportExportFormat.EXCEL,
                "Estado de Resultados",
                "ENT-TEST-001",
                rows,
                financialStatement,
                null
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelFile.content()))) {
            String sheetText = extractSheetText(workbook);

            assertThat(sheetText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel")
                    .contains("Subcuenta")
                    .contains("Fecha de Inicio Periodo Actual")
                    .contains("01/01/2025")
                    .contains("Fecha de Fin Periodo Actual")
                    .contains("29/03/2025")
                    .contains("Fecha de Inicio Periodo Anterior")
                    .contains("01/01/2024")
                    .contains("Fecha de Fin Periodo Anterior")
                    .contains("29/03/2024");
        }
    }

    @Test
    void shouldIncludeEquityCriteriaInPdfAndExcel() throws IOException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineDescription", "Capital emitido");
        row.put("rowType", "DETAIL");
        row.put("currentAmount", new BigDecimal("120000000"));
        row.put("previousAmount", new BigDecimal("100000000"));
        row.put("yearValues", Map.of(
                "2024", new BigDecimal("100000000"),
                "2025", new BigDecimal("120000000")
        ));

        Map<String, Object> totalRow = new LinkedHashMap<>();
        totalRow.put("lineDescription", "Total patrimonio de los accionistas");
        totalRow.put("rowType", "TOTAL");
        totalRow.put("currentAmount", new BigDecimal("120000000"));
        totalRow.put("previousAmount", new BigDecimal("100000000"));
        totalRow.put("yearValues", Map.of(
                "2024", new BigDecimal("100000000"),
                "2025", new BigDecimal("120000000")
        ));

        Map<String, Object> financialStatement = new LinkedHashMap<>();
        financialStatement.put("type", "STATEMENT_CHANGES_EQUITY");
        financialStatement.put("criteria", FinancialStatementCriteria.builder()
                .criteriaType("ACCOUNT")
                .startDate(LocalDate.of(2024, 3, 29))
                .endDate(LocalDate.of(2025, 3, 29))
                .build());

        FinancialStatementExportService.ExportedFile pdfFile = exportService.export(
                EReportExportFormat.PDF,
                "Estado de Cambios en el Patrimonio",
                "ENT-TEST-001",
                List.of(row, totalRow),
                financialStatement,
                null
        );

        try (PDDocument document = Loader.loadPDF(pdfFile.content())) {
            String pdfText = new PDFTextStripper().getText(document);

            assertThat(pdfText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel: Cuenta")
                    .contains("Fecha de Corte Anterior: 29/03/2024")
                    .contains("Fecha de Corte Actual: 29/03/2025");
        }

        FinancialStatementExportService.ExportedFile excelFile = exportService.export(
                EReportExportFormat.EXCEL,
                "Estado de Cambios en el Patrimonio",
                "ENT-TEST-001",
                List.of(row, totalRow),
                financialStatement,
                null
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelFile.content()))) {
            String sheetText = extractSheetText(workbook);

            assertThat(sheetText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel")
                    .contains("Cuenta")
                    .contains("Fecha de Corte Anterior")
                    .contains("29/03/2024")
                    .contains("Fecha de Corte Actual")
                    .contains("29/03/2025");
        }
    }

    private Map<String, Object> financialPositionRow(
            String lineDescription,
            String rowType,
            String currentAmount,
            String currentPercentage,
            String previousAmount,
            String previousPercentage,
            String variation,
            String variationPercentage
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineDescription", lineDescription);
        row.put("rowType", rowType);
        row.put("currentAmount", new BigDecimal(currentAmount));
        row.put("currentPercentage", new BigDecimal(currentPercentage));
        row.put("previousAmount", new BigDecimal(previousAmount));
        row.put("previousPercentage", new BigDecimal(previousPercentage));
        row.put("variation", new BigDecimal(variation));
        row.put("variationPercentage", new BigDecimal(variationPercentage));
        return row;
    }

    private String extractSheetText(XSSFWorkbook workbook) {
        StringBuilder content = new StringBuilder();
        workbook.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> {
            String value = cell.toString();
            if (!value.isBlank()) {
                content.append(value).append('\n');
            }
        })));
        return content.toString();
    }
}
