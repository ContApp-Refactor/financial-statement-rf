package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.controller.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementAnnotation;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementDeliveryPort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementDocument;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementHistoryItem;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementLog;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementTemplate;
import com.unicauca.edu.co.financial_statements.domain.models.core.PageResult;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.ResponseDTO;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.DeleteFinancialStatementTemplatesRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.ExportFinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.GenerateFinancialStatementRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.UpsertFinancialStatementAnnotationRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request.UpsertFinancialStatementTemplateRequest;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.mapper.FinancialStatementRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial-statements")
public class FinancialStatementCommandController {

    private final IFinancialStatementCommandPort financialStatementCommandPort;
    private final IFinancialStatementDeliveryPort financialStatementDeliveryPort;
    private final FinancialStatementRestMapper financialStatementRestMapper;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("El controlador de estados financieros esta funcionando");
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<FinancialStatementGenerationResult>> registerFinancialStatement(
            @Valid @RequestBody GenerateFinancialStatementRequest request) {

        FinancialStatementGenerationResult reportResult = financialStatementCommandPort
                .registerFinancialStatement(financialStatementRestMapper.toDomain(request));

        return ResponseDTO.<FinancialStatementGenerationResult>builder()
                .data(reportResult)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Estado financiero generado correctamente.")
                .build()
                .of();
    }

    @PostMapping("/preview")
    public ResponseEntity<ResponseDTO<FinancialStatementGenerationResult>> previewFinancialStatement(
            @Valid @RequestBody GenerateFinancialStatementRequest request) {

        FinancialStatementGenerationResult reportResult = financialStatementCommandPort
                .previewFinancialStatement(financialStatementRestMapper.toDomain(request));

        return ResponseDTO.<FinancialStatementGenerationResult>builder()
                .data(reportResult)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Vista previa del estado financiero generada correctamente.")
                .build()
                .of();
    }

    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<PageResult<FinancialStatementHistoryItem>>> getHistoryByEnterprise(
            @RequestParam String enterpriseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PageResult<FinancialStatementHistoryItem> historyPage = financialStatementCommandPort
                .getHistoryByEnterprise(enterpriseId, page, size, sort);

        return ResponseDTO.<PageResult<FinancialStatementHistoryItem>>builder()
                .data(historyPage)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Historial del estado financiero encontrado.")
                .build()
                .of();
    }

    @GetMapping("/logs")
    public ResponseEntity<ResponseDTO<List<FinancialStatementLog>>> getLogsByReportId(
            @RequestParam(name = "reportId", required = false) UUID reportId,
            @RequestParam(name = "financialStatementId", required = false) UUID financialStatementId
    ) {
        UUID resolvedReportId = reportId != null ? reportId : financialStatementId;
        List<FinancialStatementLog> logs = financialStatementCommandPort.getLogsByReportId(resolvedReportId);

        return ResponseDTO.<List<FinancialStatementLog>>builder()
                .data(logs)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Registros del estado financiero encontrados.")
                .build()
                .of();
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportFinancialStatement(@Valid @RequestBody ExportFinancialStatementRequest request) {
        FinancialStatementDocument exportedFile = financialStatementDeliveryPort.export(
                financialStatementRestMapper.toDomain(request)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFile.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(exportedFile.getContentType()))
                .body(exportedFile.getContent());
    }

    @PostMapping("/preview/export")
    public ResponseEntity<byte[]> exportFinancialStatementPreview(
            @Valid @RequestBody GenerateFinancialStatementRequest request,
            @RequestParam(name = "format", required = false) String format
    ) {
        FinancialStatementGenerationResult preview = financialStatementCommandPort
                .previewFinancialStatement(financialStatementRestMapper.toDomain(request));

        FinancialStatementDocument exportedFile = financialStatementDeliveryPort.export(
                financialStatementRestMapper.toPreviewExportCommand(preview, format)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFile.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(exportedFile.getContentType()))
                .body(exportedFile.getContent());
    }

    @PostMapping("/templates/default")
    public ResponseEntity<ResponseDTO<FinancialStatementTemplate>> saveDefaultTemplate(
            @Valid @RequestBody UpsertFinancialStatementTemplateRequest request
    ) {
        FinancialStatementTemplate template = financialStatementCommandPort
                .saveDefaultTemplate(financialStatementRestMapper.toDomain(request));

        return ResponseDTO.<FinancialStatementTemplate>builder()
                .data(template)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantilla del estado financiero guardada correctamente.")
                .build()
                .of();
    }

    @PostMapping("/templates")
    public ResponseEntity<ResponseDTO<FinancialStatementTemplate>> saveTemplate(
            @Valid @RequestBody UpsertFinancialStatementTemplateRequest request
    ) {
        FinancialStatementTemplate template = financialStatementCommandPort
                .saveTemplate(financialStatementRestMapper.toDomain(request));

        return ResponseDTO.<FinancialStatementTemplate>builder()
                .data(template)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantilla del estado financiero guardada correctamente.")
                .build()
                .of();
    }

    @GetMapping("/templates/default")
    public ResponseEntity<ResponseDTO<Object>> getDefaultTemplate(
            @RequestParam String enterpriseId
    ) {
        return financialStatementCommandPort.getDefaultTemplateByEnterprise(enterpriseId)
                .<ResponseEntity<ResponseDTO<Object>>>map(template -> ResponseDTO.<Object>builder()
                        .data(template)
                        .statusCode(HttpStatus.OK.value())
                        .code(HttpStatus.OK.value())
                        .message("Plantilla del estado financiero encontrada.")
                        .build()
                        .of())
                .orElseGet(() -> ResponseDTO.<Object>builder()
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .code(HttpStatus.NOT_FOUND.value())
                        .message("No se encontro la plantilla del estado financiero.")
                        .build()
                        .of());
    }

    @GetMapping("/templates")
    public ResponseEntity<ResponseDTO<List<FinancialStatementTemplate>>> getTemplates(
            @RequestParam String enterpriseId
    ) {
        List<FinancialStatementTemplate> templates = financialStatementCommandPort
                .getTemplatesByEnterprise(enterpriseId);

        return ResponseDTO.<List<FinancialStatementTemplate>>builder()
                .data(templates)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantillas del estado financiero encontradas.")
                .build()
                .of();
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<ResponseDTO<Integer>> deleteTemplate(
            @PathVariable Long templateId,
            @RequestParam String enterpriseId
    ) {
        financialStatementCommandPort.deleteTemplate(enterpriseId, templateId);

        return ResponseDTO.<Integer>builder()
                .data(1)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantilla del estado financiero eliminada correctamente.")
                .build()
                .of();
    }

    @PostMapping("/templates/delete-batch")
    public ResponseEntity<ResponseDTO<Integer>> deleteTemplates(
            @Valid @RequestBody DeleteFinancialStatementTemplatesRequest request
    ) {
        int deletedCount = financialStatementCommandPort.deleteTemplates(
                request.getEnterpriseId(),
                request.getTemplateIds()
        );

        return ResponseDTO.<Integer>builder()
                .data(deletedCount)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantillas del estado financiero eliminadas correctamente.")
                .build()
                .of();
    }

    @DeleteMapping("/templates")
    public ResponseEntity<ResponseDTO<Integer>> deleteAllTemplates(
            @RequestParam String enterpriseId
    ) {
        int deletedCount = financialStatementCommandPort.deleteAllTemplates(enterpriseId);

        return ResponseDTO.<Integer>builder()
                .data(deletedCount)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Plantillas del estado financiero eliminadas correctamente.")
                .build()
                .of();
    }

    @GetMapping("/{reportId}/annotations")
    public ResponseEntity<ResponseDTO<List<FinancialStatementAnnotation>>> getAnnotations(
            @PathVariable UUID reportId
    ) {
        List<FinancialStatementAnnotation> annotations = financialStatementCommandPort.getAnnotations(reportId);

        return ResponseDTO.<List<FinancialStatementAnnotation>>builder()
                .data(annotations)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Anotaciones del estado financiero encontradas.")
                .build()
                .of();
    }

    @PostMapping("/{reportId}/annotations")
    public ResponseEntity<ResponseDTO<FinancialStatementAnnotation>> createAnnotation(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpsertFinancialStatementAnnotationRequest request
    ) {
        FinancialStatementAnnotation annotation = financialStatementCommandPort
                .createAnnotation(reportId, request.getText());

        return ResponseDTO.<FinancialStatementAnnotation>builder()
                .data(annotation)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Anotacion del estado financiero guardada correctamente.")
                .build()
                .of();
    }

    @PutMapping("/{reportId}/annotations/{annotationId}")
    public ResponseEntity<ResponseDTO<FinancialStatementAnnotation>> updateAnnotation(
            @PathVariable UUID reportId,
            @PathVariable Long annotationId,
            @Valid @RequestBody UpsertFinancialStatementAnnotationRequest request
    ) {
        FinancialStatementAnnotation annotation = financialStatementCommandPort
                .updateAnnotation(reportId, annotationId, request.getText());

        return ResponseDTO.<FinancialStatementAnnotation>builder()
                .data(annotation)
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Anotacion del estado financiero actualizada correctamente.")
                .build()
                .of();
    }

    @DeleteMapping("/{reportId}/annotations/{annotationId}")
    public ResponseEntity<ResponseDTO<Void>> deleteAnnotation(
            @PathVariable UUID reportId,
            @PathVariable Long annotationId
    ) {
        financialStatementCommandPort.deleteAnnotation(reportId, annotationId);

        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .code(HttpStatus.OK.value())
                .message("Anotacion del estado financiero eliminada correctamente.")
                .build()
                .of();
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> downloadFinancialStatement(
            @PathVariable UUID reportId,
            @RequestParam(name = "format", required = false) String format
    ) {
        FinancialStatementDocument exportedFile = financialStatementDeliveryPort.download(
                reportId,
                financialStatementRestMapper.toExportFormat(format)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFile.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(exportedFile.getContentType()))
                .body(exportedFile.getContent());
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ResponseDTO<Object>> getFinancialStatementReport(
            @PathVariable UUID reportId) {

        return financialStatementCommandPort.getFinancialStatementSnapshot(reportId)
                .<ResponseEntity<ResponseDTO<Object>>>map(snapshot -> ResponseDTO.<Object>builder()
                        .data(snapshot)
                        .statusCode(HttpStatus.OK.value())
                        .code(HttpStatus.OK.value())
                        .message("Snapshot del estado financiero encontrado.")
                        .build()
                        .of())
                .or(() -> financialStatementCommandPort.getFinancialStatementReport(reportId)
                        .map(report -> ResponseDTO.<Object>builder()
                                .data(report)
                                .statusCode(HttpStatus.OK.value())
                                .code(HttpStatus.OK.value())
                                .message("Reporte del estado financiero encontrado.")
                                .build()
                                .of()))
                .orElseGet(() -> ResponseDTO.<Object>builder()
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .code(HttpStatus.NOT_FOUND.value())
                        .message("No se encontro el reporte del estado financiero.")
                        .build()
                        .of());
    }
}

