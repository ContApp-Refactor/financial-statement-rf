package com.unicauca.edu.co.financial_statements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.application.ports.out.IAccountingInfoClient;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo.AccountInfoClientException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.auth.enabled=false"
})
class FinancialStatementFlowIntegrationTest {

    private static final String SAMPLE_SIGNATURE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAIAAAABCAYAAAD0In+KAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAOSURBVBhXY2BgYPgPAgAO+gT8M0OZvQAAAABJRU5ErkJggg==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAccountingInfoClient accountingInfoClient;

    @BeforeEach
    void setUp() {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenReturn(buildAccountingEntries());
    }

    @Test
    void shouldRegisterGenerateAndExportFinancialStatementWithoutErrors() throws Exception {
        String registerPayload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        JsonNode registerData = registerJson.path("data");
        String reportId = registerData.path("financialStatement").path("reportId").asText();
        String downloadUrl = registerData.path("financialStatement").path("downloadUrl").asText();

        assertThat(reportId).isNotBlank();
        assertThat(downloadUrl).isNotBlank();
        assertThat(registerData.path("financialStatementData").isArray()).isTrue();
        assertThat(registerData.path("financialStatementData")).isNotEmpty();

        BigDecimal totalAssets = registerData.path("totalAssets").decimalValue();
        BigDecimal totalLiabilities = registerData.path("totalLiabilities").decimalValue();
        BigDecimal totalEquity = registerData.path("totalEquity").decimalValue();

        assertThat(totalAssets).isEqualByComparingTo(totalLiabilities.add(totalEquity));

        MvcResult directDownloadResult = mockMvc.perform(get(downloadUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn();

        byte[] directDownloadBytes = directDownloadResult.getResponse().getContentAsByteArray();
        assertThat(directDownloadBytes).isNotEmpty();
        assertThat(directDownloadResult.getResponse().getContentType()).contains("application/pdf");

        String exportPayloadPdf = """
                {
                  "reportId": "%s",
                  "format": "PDF"
                }
                """.formatted(reportId);

        MvcResult exportPdfResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportPayloadPdf))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn();

        byte[] pdfBytes = exportPdfResult.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(exportPdfResult.getResponse().getContentType()).contains("application/pdf");

        String exportPayloadExcel = """
                {
                  "reportId": "%s",
                  "format": "EXCEL"
                }
                """.formatted(reportId);

        MvcResult exportExcelResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportPayloadExcel))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn();

        byte[] excelBytes = exportExcelResult.getResponse().getContentAsByteArray();
        assertThat(excelBytes).isNotEmpty();
        assertThat(exportExcelResult.getResponse().getContentType())
                .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        MvcResult historyResult = mockMvc.perform(get("/api/financial-statements/history")
                        .param("enterpriseId", "ENT-TEST-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyJson = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(historyJson.path("data").path("content").isArray()).isTrue();
        assertThat(historyJson.path("data").path("content")).isNotEmpty();
        assertThat(historyJson.path("data").path("content").toString()).contains("GENERATED");

        MvcResult logsResult = mockMvc.perform(get("/api/financial-statements/logs")
                        .param("reportId", reportId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode logsJson = objectMapper.readTree(logsResult.getResponse().getContentAsString());
        assertThat(logsJson.path("data").isArray()).isTrue();
        assertThat(logsJson.path("data")).isNotEmpty();
        assertThat(logsJson.path("data").get(0).path("eventType").asText()).isEqualTo("EXPORTED");

        String templatePayload = """
                {
                  "enterpriseId": "ENT-TEST-001",
                  "name": "Plantilla base",
                  "alignment": "center",
                  "font": "Helvetica",
                  "fontSize": 12,
                  "mainColor": "#003366"
                }
                """;

        mockMvc.perform(post("/api/financial-statements/templates/default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templatePayload))
                .andExpect(status().isOk());

        MvcResult templateResult = mockMvc.perform(get("/api/financial-statements/templates/default")
                        .param("enterpriseId", "ENT-TEST-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode templateJson = objectMapper.readTree(templateResult.getResponse().getContentAsString());
        assertThat(templateJson.path("data").path("name").asText()).isEqualTo("Plantilla base");

        String createSchedulePayload = """
                {
                  "reportId": "%s",
                  "recipientEmail": "usuario@dominio.com",
                  "format": "PDF",
                  "frequency": "DAILY",
                  "hourOfDay": 8,
                  "minuteOfHour": 30,
                  "timezone": "America/Bogota"
                }
                """.formatted(reportId);

        MvcResult scheduleResult = mockMvc.perform(post("/api/financial-statements/email-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSchedulePayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode scheduleJson = objectMapper.readTree(scheduleResult.getResponse().getContentAsString());
        long scheduleId = scheduleJson.path("data").path("id").asLong();
        assertThat(scheduleId).isPositive();
        assertThat(scheduleJson.path("data").path("active").asBoolean()).isTrue();

        MvcResult schedulesResult = mockMvc.perform(get("/api/financial-statements/email-schedules")
                        .param("reportId", reportId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode schedulesJson = objectMapper.readTree(schedulesResult.getResponse().getContentAsString());
        assertThat(schedulesJson.path("data").isArray()).isTrue();
        assertThat(schedulesJson.path("data")).hasSize(1);

        String updateSchedulePayload = """
                {
                  "active": false
                }
                """;

        MvcResult updatedScheduleResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/financial-statements/email-schedules/{scheduleId}/status", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateSchedulePayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updatedScheduleJson = objectMapper.readTree(updatedScheduleResult.getResponse().getContentAsString());
        assertThat(updatedScheduleJson.path("data").path("active").asBoolean()).isFalse();
    }

    @Test
    void shouldAllowUpToThreeNamedTemplatesPerEnterpriseAndPreserveDefaultSelection() throws Exception {
        String templateOnePayload = """
                {
                  "enterpriseId": "ENT-TEMPLATES-001",
                  "name": "Corporativa Azul",
                  "alignment": "center",
                  "font": "Helvetica",
                  "fontSize": 12,
                  "mainColor": "#003366",
                  "isDefault": true
                }
                """;

        String templateTwoPayload = """
                {
                  "enterpriseId": "ENT-TEMPLATES-001",
                  "name": "Minimalista",
                  "alignment": "left",
                  "font": "Arial",
                  "fontSize": 10,
                  "mainColor": "#444444"
                }
                """;

        String templateThreePayload = """
                {
                  "enterpriseId": "ENT-TEMPLATES-001",
                  "name": "Contable Verde",
                  "alignment": "right",
                  "font": "Calibri",
                  "fontSize": 14,
                  "mainColor": "#008060"
                }
                """;

        mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateOnePayload))
                .andExpect(status().isOk());

        MvcResult secondTemplateResult = mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateTwoPayload))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateThreePayload))
                .andExpect(status().isOk());

        JsonNode secondTemplateJson = objectMapper.readTree(secondTemplateResult.getResponse().getContentAsString());
        long secondTemplateId = secondTemplateJson.path("data").path("id").asLong();

        String promoteSecondTemplatePayload = """
                {
                  "id": %d,
                  "enterpriseId": "ENT-TEMPLATES-001",
                  "name": "Minimalista",
                  "alignment": "left",
                  "font": "Arial",
                  "fontSize": 10,
                  "mainColor": "#444444",
                  "isDefault": true
                }
                """.formatted(secondTemplateId);

        mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promoteSecondTemplatePayload))
                .andExpect(status().isOk());

        MvcResult templatesResult = mockMvc.perform(get("/api/financial-statements/templates")
                        .param("enterpriseId", "ENT-TEMPLATES-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode templatesJson = objectMapper.readTree(templatesResult.getResponse().getContentAsString());
        JsonNode templatesData = templatesJson.path("data");

        assertThat(templatesData).hasSize(3);
        assertThat(templatesData.toString())
                .contains("Corporativa Azul")
                .contains("Minimalista")
                .contains("Contable Verde");
        assertThat(templatesData.get(0).path("name").asText()).isEqualTo("Minimalista");
        assertThat(templatesData.get(0).path("isDefault").asBoolean()).isTrue();

        MvcResult defaultTemplateResult = mockMvc.perform(get("/api/financial-statements/templates/default")
                        .param("enterpriseId", "ENT-TEMPLATES-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode defaultTemplateJson = objectMapper.readTree(defaultTemplateResult.getResponse().getContentAsString());
        assertThat(defaultTemplateJson.path("data").path("name").asText()).isEqualTo("Minimalista");

        String fourthTemplatePayload = """
                {
                  "enterpriseId": "ENT-TEMPLATES-001",
                  "name": "Extra",
                  "alignment": "center",
                  "font": "Arial",
                  "fontSize": 11,
                  "mainColor": "#111111"
                }
                """;

        MvcResult fourthTemplateResult = mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fourthTemplatePayload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode fourthTemplateJson = objectMapper.readTree(fourthTemplateResult.getResponse().getContentAsString());
        assertThat(fourthTemplateJson.path("message").asText())
                .contains("maximum of 3 templates");
    }

    @Test
    void shouldDeleteTemplatesIndividuallyInBatchAndCompletely() throws Exception {
        String enterpriseId = "ENT-TEMPLATE-DELETE-001";

        long templateOneId = createTemplate(enterpriseId, "Predeterminada", true);
        long templateTwoId = createTemplate(enterpriseId, "Secundaria", false);
        long templateThreeId = createTemplate(enterpriseId, "Temporal", false);

        mockMvc.perform(delete("/api/financial-statements/templates/{templateId}", templateOneId)
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isOk());

        MvcResult defaultAfterDeleteResult = mockMvc.perform(get("/api/financial-statements/templates/default")
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode defaultAfterDeleteJson = objectMapper.readTree(defaultAfterDeleteResult.getResponse().getContentAsString());
        assertThat(defaultAfterDeleteJson.path("data").path("id").asLong()).isEqualTo(templateTwoId);
        assertThat(defaultAfterDeleteJson.path("data").path("isDefault").asBoolean()).isTrue();

        String deleteBatchPayload = """
                {
                  "enterpriseId": "%s",
                  "templateIds": [%d]
                }
                """.formatted(enterpriseId, templateThreeId);

        mockMvc.perform(post("/api/financial-statements/templates/delete-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteBatchPayload))
                .andExpect(status().isOk());

        MvcResult templatesAfterBatchDelete = mockMvc.perform(get("/api/financial-statements/templates")
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode templatesAfterBatchDeleteJson = objectMapper.readTree(templatesAfterBatchDelete.getResponse().getContentAsString());
        assertThat(templatesAfterBatchDeleteJson.path("data")).hasSize(1);

        mockMvc.perform(delete("/api/financial-statements/templates")
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isOk());

        MvcResult templatesAfterDeleteAll = mockMvc.perform(get("/api/financial-statements/templates")
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode templatesAfterDeleteAllJson = objectMapper.readTree(templatesAfterDeleteAll.getResponse().getContentAsString());
        assertThat(templatesAfterDeleteAllJson.path("data")).isEmpty();

        mockMvc.perform(get("/api/financial-statements/templates/default")
                        .param("enterpriseId", enterpriseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPersistListAndExportAnnotations() throws Exception {
        String reportId = registerFinancialPositionReport("ENT-ANNOTATIONS-001");

        String blankAnnotationPayload = """
                {
                  "text": ""
                }
                """;

        MvcResult blankAnnotationResult = mockMvc.perform(post("/api/financial-statements/{reportId}/annotations", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankAnnotationPayload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode blankAnnotationJson = objectMapper.readTree(blankAnnotationResult.getResponse().getContentAsString());
        assertThat(blankAnnotationJson.path("message").asText())
                .isEqualTo("Debe escribir una anotación antes de guardar");

        String createAnnotationPayload = """
                {
                  "text": "Primera anotacion importante del reporte"
                }
                """;

        MvcResult createdAnnotationResult = mockMvc.perform(post("/api/financial-statements/{reportId}/annotations", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAnnotationPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createdAnnotationJson = objectMapper.readTree(createdAnnotationResult.getResponse().getContentAsString());
        long annotationId = createdAnnotationJson.path("data").path("id").asLong();
        assertThat(annotationId).isPositive();

        String updateAnnotationPayload = """
                {
                  "text": "Anotacion actualizada para exportacion"
                }
                """;

        mockMvc.perform(put("/api/financial-statements/{reportId}/annotations/{annotationId}", reportId, annotationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAnnotationPayload))
                .andExpect(status().isOk());

        MvcResult snapshotResult = mockMvc.perform(get("/api/financial-statements/{reportId}", reportId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode snapshotJson = objectMapper.readTree(snapshotResult.getResponse().getContentAsString());
        assertThat(snapshotJson.path("data").path("annotations")).hasSize(1);
        assertThat(snapshotJson.path("data").path("annotations").get(0).path("text").asText())
                .isEqualTo("Anotacion actualizada para exportacion");

        String exportPdfPayload = """
                {
                  "reportId": "%s",
                  "format": "PDF"
                }
                """.formatted(reportId);

        MvcResult exportPdfResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportPdfPayload))
                .andExpect(status().isOk())
                .andReturn();

        try (PDDocument document = Loader.loadPDF(exportPdfResult.getResponse().getContentAsByteArray())) {
            String pdfText = new PDFTextStripper().getText(document);
            assertThat(pdfText)
                    .contains("ANOTACIONES")
                    .contains("Anotacion actualizada para exportacion");
        }

        String exportExcelPayload = """
                {
                  "reportId": "%s",
                  "format": "EXCEL"
                }
                """.formatted(reportId);

        MvcResult exportExcelResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportExcelPayload))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportExcelResult.getResponse().getContentAsByteArray()))) {
            String sheetText = extractSheetText(workbook);
            assertThat(sheetText)
                    .contains("ANOTACIONES")
                    .contains("Anotacion actualizada para exportacion");
        }
    }

    @Test
    void shouldExportWithVisualSignatureAndRejectInvalidSignatureType() throws Exception {
        String reportId = registerFinancialPositionReport("ENT-SIGNATURE-001");

        String exportWithSignaturePayload = """
                {
                  "reportId": "%s",
                  "format": "PDF",
                  "signature": {
                    "fileName": "firma.png",
                    "contentType": "image/png",
                    "base64Content": "%s"
                  }
                }
                """.formatted(reportId, SAMPLE_SIGNATURE_BASE64);

        mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportWithSignaturePayload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));

        String invalidSignaturePayload = """
                {
                  "reportId": "%s",
                  "format": "PDF",
                  "signature": {
                    "fileName": "firma.pdf",
                    "contentType": "application/pdf",
                    "base64Content": "%s"
                  }
                }
                """.formatted(reportId, SAMPLE_SIGNATURE_BASE64);

        MvcResult invalidSignatureResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSignaturePayload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode invalidSignatureJson = objectMapper.readTree(invalidSignatureResult.getResponse().getContentAsString());
        assertThat(invalidSignatureJson.path("message").asText())
                .isEqualTo("Debe seleccionar un archivo de firma válido");
    }

    @Test
    void shouldReturnFriendlyErrorAndAvoidHistoryWhenGenerationFails() throws Exception {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenThrow(new AccountInfoClientException(HttpStatus.BAD_GATEWAY, "integration failure"));

        String payload = """
                {
                  "entId": "ENT-FAIL-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult failedResult = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadGateway())
                .andReturn();

        JsonNode failedJson = objectMapper.readTree(failedResult.getResponse().getContentAsString());
        assertThat(failedJson.path("message").asText())
                .isEqualTo("Error al generar el reporte. Intente más tarde");

        MvcResult historyResult = mockMvc.perform(get("/api/financial-statements/history")
                        .param("enterpriseId", "ENT-FAIL-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyJson = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(historyJson.path("data").path("content")).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenCutoffDateFormatIsInvalid() throws Exception {
        String invalidPayload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2026-03-1",
                    "currentCutoffDate": "2026-03-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("statusCode").asInt()).isEqualTo(400);
        assertThat(body.path("message").asText()).contains("yyyy-MM-dd or dd/MM/yyyy");
    }

    @Test
    void shouldRegisterFinancialPositionWithoutNestedCriteriaObject() throws Exception {
        String payload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "previousCutoffDate": "2025-12-31",
                  "currentCutoffDate": "2026-12-31"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("financialStatement").path("criteria").path("previousCutoffDate").asText())
                .isEqualTo("2025-12-31");
        assertThat(body.path("data").path("financialStatement").path("criteria").path("currentCutoffDate").asText())
                .isEqualTo("2026-12-31");
    }

    @Test
    void shouldRegisterFinancialPositionWithUiDateFormat() throws Exception {
        String payload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "31/12/2025",
                    "currentCutoffDate": "31/12/2026"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("financialStatement").path("criteria").path("previousCutoffDate").asText())
                .isEqualTo("2025-12-31");
        assertThat(body.path("data").path("financialStatement").path("criteria").path("currentCutoffDate").asText())
                .isEqualTo("2026-12-31");
    }

    @Test
    void shouldReturnBadRequestWhenBothCutoffDatesAreAfterLatestMovementDate() throws Exception {
        String payload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2027-01-01",
                    "currentCutoffDate": "2027-12-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("statusCode").asInt()).isEqualTo(400);
        assertThat(body.path("message").asText())
                .contains("latest accounting movement date");
    }

    @Test
    void shouldReturnBadRequestWhenFinancialPositionDoesNotBalance() throws Exception {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenReturn(buildUnbalancedAccountingEntries());

        String payload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("message").asText())
                .contains("does not balance")
                .contains("Review the accounting source");
    }

    @Test
    void shouldPreserveLevelCriteriaAndExposeLevelRowsInSnapshotsAndHistory() throws Exception {
        String payload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "criteriaType": "GROUP",
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        JsonNode registerData = registerJson.path("data");
        String reportId = registerData.path("financialStatement").path("reportId").asText();

        assertThat(registerData.path("financialStatement").path("criteria").path("criteriaType").asText())
                .isEqualTo("GROUP");
        assertThat(registerData.path("financialStatementData").toString())
                .contains("\"accountCode\":\"11\"")
                .contains("\"accountCode\":\"13\"")
                .contains("\"accountCode\":\"31\"");

        MvcResult snapshotResult = mockMvc.perform(get("/api/financial-statements/{reportId}", reportId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode snapshotJson = objectMapper.readTree(snapshotResult.getResponse().getContentAsString());
        assertThat(snapshotJson.path("data").path("financialStatement").path("criteria").path("criteriaType").asText())
                .isEqualTo("GROUP");

        MvcResult historyResult = mockMvc.perform(get("/api/financial-statements/history")
                        .param("enterpriseId", "ENT-TEST-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyJson = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(historyJson.path("data").path("content").get(0).path("criteria").path("criteriaType").asText())
                .isEqualTo("GROUP");
    }

    @Test
    void shouldAllowBlankLevelCriteriaWithoutApplyingLevelFilter() throws Exception {
        String payload = """
                {
                  "entId": "ENT-LEVEL-OPTIONAL-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "criteriaType": "   ",
                    "criteriaRange": {
                      "from": 11,
                      "to": 11
                    },
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode registerData = json.path("data");
        JsonNode criteriaTypeNode = registerData.path("financialStatement").path("criteria").path("criteriaType");

        assertThat(criteriaTypeNode.isMissingNode() || criteriaTypeNode.isNull()).isTrue();
        assertThat(registerData.path("financialStatementData").toString())
                .doesNotContain("\"accountCode\":\"11\"")
                .contains("TOTAL ACTIVOS");
    }

    @Test
    void shouldGeneratePreviewWithSelectedLevelWithoutPersistingHistory() throws Exception {
        String payload = """
                {
                  "entId": "ENT-PREVIEW-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "criteriaType": "GROUP",
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult previewResult = mockMvc.perform(post("/api/financial-statements/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode previewJson = objectMapper.readTree(previewResult.getResponse().getContentAsString());
        JsonNode previewData = previewJson.path("data");
        JsonNode previewReport = previewData.path("financialStatement");

        assertThat(previewData.path("financialStatementData").toString())
                .contains("\"accountCode\":\"11\"")
                .contains("\"accountCode\":\"13\"")
                .contains("\"accountCode\":\"31\"");
        assertThat(previewReport.path("criteria").path("criteriaType").asText()).isEqualTo("GROUP");
        assertThat(previewReport.path("reportId").isMissingNode() || previewReport.path("reportId").isNull()).isTrue();
        assertThat(previewReport.path("downloadUrl").isMissingNode() || previewReport.path("downloadUrl").isNull()).isTrue();

        MvcResult historyResult = mockMvc.perform(get("/api/financial-statements/history")
                        .param("enterpriseId", "ENT-PREVIEW-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyJson = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(historyJson.path("data").path("content").isArray()).isTrue();
        assertThat(historyJson.path("data").path("content")).isEmpty();
    }

    @Test
    void shouldExportPreviewWithLevelRowsMatchingFrontendPreview() throws Exception {
        String payload = """
                {
                  "entId": "ENT-PREVIEW-EXPORT-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "criteriaType": "GROUP",
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/preview/export")
                        .param("format", "EXCEL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn();

        byte[] excelBytes = result.getResponse().getContentAsByteArray();
        assertThat(excelBytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            String sheetText = extractSheetText(workbook);
            assertThat(sheetText)
                    .contains("11 -")
                    .contains("13 -")
                    .contains("31 -");
        }
    }

    @Test
    void shouldPreferReportEnterpriseIdOverFrontendEnterpriseNameDuringExport() throws Exception {
        String registerPayload = """
                {
                  "entId": "ENT-TEST-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String reportId = registerJson.path("data").path("financialStatement").path("reportId").asText();

        String exportPayload = """
                {
                  "reportId": "%s",
                  "format": "EXCEL",
                  "entName": "Pepsi"
                }
                """.formatted(reportId);

        MvcResult exportResult = mockMvc.perform(post("/api/financial-statements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportPayload))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportResult.getResponse().getContentAsByteArray()))) {
            String sheetText = extractSheetText(workbook);
            assertThat(sheetText)
                    .contains("ENT-TEST-001")
                    .doesNotContain("Pepsi");
        }
    }

    @Test
    void shouldKeepAssetAndLiabilityAccountsInTheirCorrectSectionsForLevelRows() throws Exception {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenReturn(buildClassificationEdgeCaseEntries());

        String payload = """
                {
                  "entId": "ENT-CLASS-CHECK-001",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "criteriaType": "ACCOUNT",
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("financialStatementData");

        assertThat(findRowByDescription(rows, "Activos por impuestos corrientes").path("currentAmount").decimalValue())
                .isEqualByComparingTo("110");
        assertThat(findRowByDescription(rows, "Pasivos por impuestos corrientes").path("currentAmount").decimalValue())
                .isEqualByComparingTo("45");
        assertThat(findRowByDescription(rows, "Pasivos por impuestos diferidos").path("currentAmount").decimalValue())
                .isEqualByComparingTo("25");
        assertThat(findRowByDescription(rows, "Otros activos").path("currentAmount").decimalValue())
                .isEqualByComparingTo("70");

        String assetTaxSection = String.join("\n",
                extractLineDescriptionsBetween(rows, "Activos por impuestos corrientes", "Activos biologicos"));
        assertThat(assetTaxSection)
                .contains("1355 -")
                .doesNotContain("2408 -");

        String otherAssetsSection = String.join("\n",
                extractLineDescriptionsBetween(rows, "Otros activos", "TOTAL ACTIVO NO CORRIENTE"));
        assertThat(otherAssetsSection)
                .contains("1705 -")
                .doesNotContain("2705 -");

        String currentTaxLiabilitySection = String.join("\n",
                extractLineDescriptionsBetween(rows, "Pasivos por impuestos corrientes", "Provision"));
        assertThat(currentTaxLiabilitySection)
                .contains("2408 -")
                .doesNotContain("1355 -");

        String deferredTaxLiabilitySection = String.join("\n",
                extractLineDescriptionsBetween(rows, "Pasivos por impuestos diferidos", "TOTAL PASIVO NO CORRIENTE"));
        assertThat(deferredTaxLiabilitySection)
                .contains("2705 -")
                .doesNotContain("1705 -");
    }

    @Test
    void shouldUseOrdinaryIncomeAsIncomeStatementPercentageBase() throws Exception {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenReturn(buildIncomeStatementPercentageEntries());

        String payload = """
                {
                  "entId": "ENT-INCOME-BASE-001",
                  "type": "INCOME_STATEMENT",
                  "criteria": {
                    "criteriaType": "SUB_ACCOUNT",
                    "startDate": "2026-01-01",
                    "endDate": "2026-03-31",
                    "previousStartDate": "2025-01-01",
                    "previousEndDate": "2025-03-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("financialStatementData");

        assertThat(findRowByDescription(rows, "Ingresos ordinarios").path("currentPercentage").decimalValue())
                .isEqualByComparingTo("100.00");
        assertThat(findRowByDescription(rows, "Ingresos ordinarios").path("previousPercentage").decimalValue())
                .isEqualByComparingTo("100.00");
        assertThat(findRowByDescription(rows, "(-) Devoluciones en ventas").path("currentPercentage").decimalValue())
                .isEqualByComparingTo("10.00");
        assertThat(findRowByDescription(rows, "(-) Devoluciones en ventas").path("previousPercentage").decimalValue())
                .isEqualByComparingTo("10.00");
        assertThat(findRowByDescription(rows, "INGRESOS NETOS OPERACIONALES").path("currentPercentage").decimalValue())
                .isEqualByComparingTo("90.00");
        assertThat(findRowByDescription(rows, "INGRESOS NETOS OPERACIONALES").path("previousPercentage").decimalValue())
                .isEqualByComparingTo("90.00");
    }

    @Test
    void shouldIncludeIncomeStatementCriteriaInPreviewPdfExport() throws Exception {
        when(accountingInfoClient.findAccountingEntries(anyString(), any(), any()))
                .thenReturn(buildIncomeStatementPercentageEntries());

        String payload = """
                {
                  "entId": "ENT-INCOME-EXPORT-001",
                  "type": "INCOME_STATEMENT",
                  "criteria": {
                    "criteriaType": "SUB_ACCOUNT",
                    "startDate": "2026-01-01",
                    "endDate": "2026-03-31",
                    "previousStartDate": "2025-01-01",
                    "previousEndDate": "2025-03-31"
                  }
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/financial-statements/preview/export")
                        .param("format", "PDF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String pdfText = new PDFTextStripper().getText(document);

            assertThat(pdfText)
                    .contains("Criterios Utilizados:")
                    .contains("Tipo de Nivel: Subcuenta")
                    .contains("Fecha de Inicio Periodo Actual: 01/01/2026")
                    .contains("Fecha de Fin Periodo Actual: 31/03/2026")
                    .contains("Fecha de Inicio Periodo Anterior: 01/01/2025")
                    .contains("Fecha de Fin Periodo Anterior: 31/03/2025");
        }
    }

    private List<AccountingEntry> buildAccountingEntries() {
        return List.of(
                // Previous cutoff entries (<= 2025-12-31)
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "1105", "Efectivo", "DEBITO", "1000.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "2105", "Acreedores comerciales", "CREDITO", "0", "300.00"),
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "3105", "Capital suscrito", "CREDITO", "0", "700.00"),

                // Current period entries (<= 2026-12-31)
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "1305", "Deudores comerciales", "DEBITO", "200.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "1504", "Propiedad, planta y equipo", "DEBITO", "500.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "2305", "Pasivos financieros largo plazo", "CREDITO", "0", "150.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "3305", "Reservas", "CREDITO", "0", "150.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "3605", "Utilidades acumuladas", "CREDITO", "0", "80.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-20"), "4135", "Ingresos operacionales", "CREDITO", "0", "320.00")
        );
    }

    private List<AccountingEntry> buildClassificationEdgeCaseEntries() {
        return List.of(
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "110505", "Activo corriente - Caja", "DEBITO", "100.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "135515", "Activos por impuestos corrientes", "DEBITO", "50.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "170505", "Otros activos diferidos", "DEBITO", "30.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "240805", "Pasivos por impuestos corrientes", "CREDITO", "0", "20.00"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "270505", "Pasivos por impuestos diferidos", "CREDITO", "0", "10.00"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2025-06-30"), "310505", "Patrimonio - Capital social", "CREDITO", "0", "150.00"),

                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "110505", "Activo corriente - Caja", "DEBITO", "120.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "135515", "Activos por impuestos corrientes", "DEBITO", "60.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "170505", "Otros activos diferidos", "DEBITO", "40.00", "0"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "240805", "Pasivos por impuestos corrientes", "CREDITO", "0", "25.00"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "270505", "Pasivos por impuestos diferidos", "CREDITO", "0", "15.00"),
                entry("ENT-CLASS-CHECK-001", LocalDate.parse("2026-03-15"), "310505", "Patrimonio - Capital social", "CREDITO", "0", "180.00")
        );
    }

    private List<AccountingEntry> buildIncomeStatementPercentageEntries() {
        return List.of(
                entry("ENT-INCOME-BASE-001", LocalDate.parse("2025-01-15"), "413505", "Ingresos operacionales - Ventas nacionales", "CREDITO", "0", "80.00"),
                entry("ENT-INCOME-BASE-001", LocalDate.parse("2025-02-15"), "417505", "Ingresos operacionales - Devoluciones en ventas", "DEBITO", "8.00", "0"),
                entry("ENT-INCOME-BASE-001", LocalDate.parse("2025-03-15"), "613505", "Costo de ventas", "DEBITO", "40.00", "0"),

                entry("ENT-INCOME-BASE-001", LocalDate.parse("2026-01-15"), "413505", "Ingresos operacionales - Ventas nacionales", "CREDITO", "0", "100.00"),
                entry("ENT-INCOME-BASE-001", LocalDate.parse("2026-02-15"), "417505", "Ingresos operacionales - Devoluciones en ventas", "DEBITO", "10.00", "0"),
                entry("ENT-INCOME-BASE-001", LocalDate.parse("2026-03-15"), "613505", "Costo de ventas", "DEBITO", "50.00", "0")
        );
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

    private JsonNode findRowByDescription(JsonNode rows, String description) {
        for (JsonNode row : rows) {
            if (description.equals(row.path("lineDescription").asText())) {
                return row;
            }
        }
        throw new IllegalArgumentException("Row not found for description: " + description);
    }

    private List<String> extractLineDescriptionsBetween(JsonNode rows, String startDescription, String endDescription) {
        List<String> descriptions = new java.util.ArrayList<>();
        boolean collecting = false;

        for (JsonNode row : rows) {
            String lineDescription = row.path("lineDescription").asText();
            if (startDescription.equals(lineDescription)) {
                collecting = true;
                continue;
            }
            if (collecting && endDescription.equals(lineDescription)) {
                break;
            }
            if (collecting) {
                descriptions.add(lineDescription);
            }
        }

        return descriptions;
    }

    private List<AccountingEntry> buildUnbalancedAccountingEntries() {
        return List.of(
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "1105", "Efectivo", "DEBITO", "1000.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "2105", "Acreedores comerciales", "CREDITO", "0", "300.00"),
                entry("ENT-TEST-001", LocalDate.parse("2025-06-30"), "3105", "Capital suscrito", "CREDITO", "0", "700.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "1305", "Deudores comerciales", "DEBITO", "200.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "1504", "Propiedad, planta y equipo", "DEBITO", "500.00", "0"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "2305", "Pasivos financieros largo plazo", "CREDITO", "0", "150.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "3305", "Reservas", "CREDITO", "0", "150.00"),
                entry("ENT-TEST-001", LocalDate.parse("2026-03-15"), "3605", "Utilidades acumuladas", "CREDITO", "0", "80.00")
        );
    }

    private AccountingEntry entry(
            String entId,
            LocalDate date,
            String accountCode,
            String accountName,
            String nature,
            String debit,
            String credit
    ) {
        return AccountingEntry.builder()
                .entId(entId)
                .date(date)
                .accountCode(accountCode)
                .accountName(accountName)
                .accountNature(nature)
                .debit(new BigDecimal(debit))
                .credit(new BigDecimal(credit))
                .movementDescription("Integration test entry")
                .build();
    }

    private long createTemplate(String enterpriseId, String name, boolean isDefault) throws Exception {
        String payload = """
                {
                  "enterpriseId": "%s",
                  "name": "%s",
                  "alignment": "center",
                  "font": "Helvetica",
                  "fontSize": 12,
                  "mainColor": "#003366",
                  "isDefault": %s
                }
                """.formatted(enterpriseId, name, isDefault);

        MvcResult result = mockMvc.perform(post("/api/financial-statements/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private String registerFinancialPositionReport(String entId) throws Exception {
        String payload = """
                {
                  "entId": "%s",
                  "type": "STATEMENT_FINANCIAL_POSITION",
                  "criteria": {
                    "previousCutoffDate": "2025-12-31",
                    "currentCutoffDate": "2026-12-31"
                  }
                }
                """.formatted(entId);

        MvcResult result = mockMvc.perform(post("/api/financial-statements/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("financialStatement")
                .path("reportId")
                .asText();
    }
}
