package com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper;

import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementExportService;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.util.Locale;

@Component
public class FinancialStatementJasperStyleResolver {

    private static final Color DEFAULT_HEADER_COLOR = new Color(45, 55, 72);
    private static final Color DEFAULT_TEXT_COLOR = new Color(0, 0, 0);
    private static final Color DEFAULT_HEADER_TEXT_COLOR = new Color(255, 255, 255);

    public Color resolveHeaderColor(FinancialStatementExportService.ExportStyle exportStyle) {
        String color = exportStyle != null ? exportStyle.mainColor() : null;
        if (!StringUtils.hasText(color)) {
            return DEFAULT_HEADER_COLOR;
        }

        String hex = color.trim().replace("#", "");
        if (hex.length() == 3) {
            hex = ""
                    + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }

        if (hex.length() != 6) {
            return DEFAULT_HEADER_COLOR;
        }

        try {
            return new Color(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            );
        } catch (NumberFormatException ignored) {
            return DEFAULT_HEADER_COLOR;
        }
    }

    public Color resolveHeaderTextColor(Color backgroundColor) {
        double luminosity = (backgroundColor.getRed() * 299
                + backgroundColor.getGreen() * 587
                + backgroundColor.getBlue() * 114) / 1000d;
        return luminosity < 128d ? DEFAULT_HEADER_TEXT_COLOR : DEFAULT_TEXT_COLOR;
    }

    public float resolveFontSize(FinancialStatementExportService.ExportStyle exportStyle, float defaultSize) {
        if (exportStyle == null || exportStyle.fontSize() == null) {
            return defaultSize;
        }
        return Math.max(8f, Math.min(14f, exportStyle.fontSize()));
    }

    public String resolveFontName(FinancialStatementExportService.ExportStyle exportStyle) {
        if (exportStyle == null || !StringUtils.hasText(exportStyle.font())) {
            return "SansSerif";
        }

        String normalized = exportStyle.font().trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("times")) {
            return "Serif";
        }
        if (normalized.contains("courier")) {
            return "Monospaced";
        }
        if (normalized.contains("helvetica")
                || normalized.contains("arial")
                || normalized.contains("calibri")
                || normalized.contains("verdana")
                || normalized.contains("tahoma")) {
            return "SansSerif";
        }
        return exportStyle.font().trim();
    }

    public HorizontalTextAlignEnum resolveTitleAlignment(FinancialStatementExportService.ExportStyle exportStyle) {
        if (exportStyle == null || !StringUtils.hasText(exportStyle.alignment())) {
            return HorizontalTextAlignEnum.LEFT;
        }

        String normalized = exportStyle.alignment().trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CENTER" -> HorizontalTextAlignEnum.CENTER;
            case "RIGHT" -> HorizontalTextAlignEnum.RIGHT;
            default -> HorizontalTextAlignEnum.LEFT;
        };
    }

    public String resolveLogoPath(FinancialStatementExportService.ExportStyle exportStyle) {
        if (exportStyle == null || !StringUtils.hasText(exportStyle.pathLogotype())) {
            return null;
        }
        return exportStyle.pathLogotype().trim();
    }
}
