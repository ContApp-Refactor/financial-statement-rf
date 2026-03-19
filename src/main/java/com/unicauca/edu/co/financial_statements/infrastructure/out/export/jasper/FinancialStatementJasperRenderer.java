package com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper;

import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementSignatureException;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementDocumentRenderer;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementExportService;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementSignatureBlock;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementTableModel;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinancialStatementJasperRenderer implements FinancialStatementDocumentRenderer {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final FinancialStatementJasperDesignFactory designFactory;
    private final FinancialStatementJasperStyleResolver styleResolver;

    @Override
    public FinancialStatementExportService.ExportedFile export(
            EReportExportFormat format,
            FinancialStatementTableModel model,
            FinancialStatementExportService.ExportStyle exportStyle
    ) {
        try {
            JasperReport report = JasperCompileManager.compileReport(designFactory.create(model, exportStyle));
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    buildParameters(model, exportStyle),
                    new JRMapCollectionDataSource((java.util.Collection) model.rows())
            );

            return switch (format) {
                case PDF -> exportPdf(model.reportName(), print);
                case EXCEL -> exportExcel(model.reportName(), print);
            };
        } catch (JRException | IOException exception) {
            if (hasAnySignature(model.signatures())) {
                throw new FinancialStatementSignatureException(
                        "Error al aplicar la firma. Puede intentar de nuevo o exportar sin firma",
                        exception
                );
            }
            throw new IllegalStateException(
                    "No fue posible exportar el archivo del estado financiero con JasperReports.",
                    exception
            );
        }
    }

    private Map<String, Object> buildParameters(
            FinancialStatementTableModel model,
            FinancialStatementExportService.ExportStyle exportStyle
    ) throws IOException {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("REPORT_NAME", model.reportName());
        parameters.put("ENTERPRISE_NAME", model.enterpriseName());
        parameters.put("GENERATED_AT", "Generado: " + model.generatedAt());
        parameters.put("CRITERIA_TEXT", model.criteriaText());
        parameters.put("LOGO_PATH", styleResolver.resolveLogoPath(exportStyle));
        putSignatureParameters(parameters, model.signatures());
        return parameters;
    }

    private void putSignatureParameters(
            Map<String, Object> parameters,
            List<FinancialStatementSignatureBlock> signatures
    ) throws IOException {
        List<FinancialStatementSignatureBlock> safeSignatures = signatures != null ? signatures : List.of();

        for (int index = 0; index < 2; index++) {
            FinancialStatementSignatureBlock signature = index < safeSignatures.size()
                    ? safeSignatures.get(index)
                    : null;
            int signatureNumber = index + 1;

            parameters.put(
                    "SIGNATURE_IMAGE_" + signatureNumber,
                    toSignatureImage(signature != null ? signature.image() : null)
            );
            parameters.put(
                    "SIGNATURE_NAME_" + signatureNumber,
                    signature != null && StringUtils.hasText(signature.signerName())
                            ? signature.signerName().trim()
                            : ""
            );
            parameters.put(
                    "SIGNATURE_ROLE_" + signatureNumber,
                    signature != null && StringUtils.hasText(signature.signerRole())
                            ? signature.signerRole().trim()
                            : ""
            );
        }
    }

    private BufferedImage toSignatureImage(byte[] signatureImage) throws IOException {
        if (signatureImage == null || signatureImage.length == 0) {
            return null;
        }

        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(signatureImage));
        if (image == null) {
            throw new IOException("No fue posible decodificar la imagen de la firma visual.");
        }
        return image;
    }

    private FinancialStatementExportService.ExportedFile exportPdf(
            String reportName,
            JasperPrint print
    ) throws JRException {
        return new FinancialStatementExportService.ExportedFile(
                JasperExportManager.exportReportToPdf(print),
                MediaType.APPLICATION_PDF,
                sanitizeFileName(reportName) + "_" + FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now()) + ".pdf",
                EReportExportFormat.PDF
        );
    }

    private FinancialStatementExportService.ExportedFile exportExcel(
            String reportName,
            JasperPrint print
    ) throws JRException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(false);
            configuration.setCollapseRowSpan(false);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setRemoveEmptySpaceBetweenColumns(true);
            exporter.setConfiguration(configuration);

            exporter.exportReport();

            return new FinancialStatementExportService.ExportedFile(
                    outputStream.toByteArray(),
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    sanitizeFileName(reportName) + "_" + FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now()) + ".xlsx",
                    EReportExportFormat.EXCEL
            );
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible escribir la exportacion de Excel con Jasper.", exception);
        }
    }

    private String sanitizeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "financial_statement";
        }
        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private boolean hasAnySignature(List<FinancialStatementSignatureBlock> signatures) {
        return signatures != null
                && signatures.stream().anyMatch(signature ->
                signature != null && signature.image() != null && signature.image().length > 0
        );
    }
}
