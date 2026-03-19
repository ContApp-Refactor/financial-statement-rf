package com.unicauca.edu.co.financial_statements.infrastructure.out.export.jasper;

import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementExportService;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementSignatureBlock;
import com.unicauca.edu.co.financial_statements.infrastructure.out.export.FinancialStatementTableModel;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinancialStatementJasperDesignFactory {

    private static final int DETAIL_ROW_HEIGHT = 20;
    private static final int HEADER_ROW_HEIGHT = 22;
    private static final int DESIGN_WIDTH = 732;
    private static final Color SECTION_BACKGROUND = new Color(245, 245, 245);

    private final ResourceLoader resourceLoader;
    private final FinancialStatementJasperStyleResolver styleResolver;

    public JasperDesign create(
            FinancialStatementTableModel model,
            FinancialStatementExportService.ExportStyle exportStyle
    ) throws JRException {
        JasperDesign design = loadBaseDesign();

        int columnCount = model.columns().size();
        for (int index = 1; index <= columnCount; index++) {
            addField(design, "column" + index);
        }
        addField(design, "rowType");
        addParameter(design, "SIGNATURE_IMAGE_1", Image.class);
        addParameter(design, "SIGNATURE_NAME_1", String.class);
        addParameter(design, "SIGNATURE_ROLE_1", String.class);
        addParameter(design, "SIGNATURE_IMAGE_2", Image.class);
        addParameter(design, "SIGNATURE_NAME_2", String.class);
        addParameter(design, "SIGNATURE_ROLE_2", String.class);

        float baseFontSize = styleResolver.resolveFontSize(exportStyle, 9f);
        String fontName = styleResolver.resolveFontName(exportStyle);
        Color headerColor = styleResolver.resolveHeaderColor(exportStyle);
        Color headerTextColor = styleResolver.resolveHeaderTextColor(headerColor);
        HorizontalTextAlignEnum titleAlignment = styleResolver.resolveTitleAlignment(exportStyle);

        applyBaseTemplateStyle(
                design,
                "EnterpriseTitle",
                fontName,
                Math.max(10f, baseFontSize + 1f),
                titleAlignment,
                headerColor
        );
        applyBaseTemplateStyle(
                design,
                "ReportTitle",
                fontName,
                Math.max(12f, baseFontSize + 2f),
                titleAlignment,
                headerColor
        );
        applyBaseTemplateStyle(
                design,
                "Metadata",
                fontName,
                Math.max(8f, baseFontSize - 1f),
                titleAlignment,
                Color.DARK_GRAY
        );
        applyBaseTemplateStyle(
                design,
                "Criteria",
                fontName,
                Math.max(8f, baseFontSize),
                titleAlignment,
                Color.DARK_GRAY
        );

        JRDesignStyle headerStyle = createHeaderStyle(design, fontName, baseFontSize, headerColor, headerTextColor);
        JRDesignStyle textStyle = createTextStyle(design, "TableText", fontName, baseFontSize, HorizontalTextAlignEnum.LEFT);
        JRDesignStyle numericStyle = createTextStyle(design, "TableNumeric", fontName, baseFontSize, HorizontalTextAlignEnum.RIGHT);

        design.setColumnHeader(createColumnHeaderBand(model.columns(), headerStyle));
        ((JRDesignSection) design.getDetailSection()).addBand(createDetailBand(columnCount, textStyle, numericStyle));
        design.setSummary(createSummaryBand(design, model.signatures(), textStyle));

        return design;
    }

    private JasperDesign loadBaseDesign() throws JRException {
        try (InputStream inputStream = resourceLoader
                .getResource("classpath:jasper/financial_statement_base.jrxml")
                .getInputStream()) {
            return JRXmlLoader.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible cargar la plantilla base de Jasper.", exception);
        }
    }

    private JRDesignBand createColumnHeaderBand(List<String> columns, JRDesignStyle headerStyle) {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(HEADER_ROW_HEIGHT);

        int[] widths = resolveColumnWidths(columns.size(), DESIGN_WIDTH);
        int x = 0;
        for (int index = 0; index < columns.size(); index++) {
            JRDesignStaticText header = new JRDesignStaticText();
            header.setX(x);
            header.setY(0);
            header.setWidth(widths[index]);
            header.setHeight(HEADER_ROW_HEIGHT);
            header.setText(columns.get(index));
            header.setStyle(headerStyle);
            applyThinBorder(header);
            band.addElement(header);
            x += widths[index];
        }
        return band;
    }

    private JRDesignBand createDetailBand(
            int columnCount,
            JRDesignStyle textStyle,
            JRDesignStyle numericStyle
    ) throws JRException {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(DETAIL_ROW_HEIGHT);
        band.setSplitType(SplitTypeEnum.STRETCH);

        int[] widths = resolveColumnWidths(columnCount, DESIGN_WIDTH);
        int x = 0;
        for (int index = 0; index < columnCount; index++) {
            JRDesignTextField emphasizedField = textField(
                    "$F{column" + (index + 1) + "}",
                    x,
                    0,
                    widths[index],
                    DETAIL_ROW_HEIGHT,
                    index == 0 ? textStyle : numericStyle
            );
            emphasizedField.setBlankWhenNull(true);
            emphasizedField.setMode(ModeEnum.OPAQUE);
            emphasizedField.setBackcolor(SECTION_BACKGROUND);
            emphasizedField.setPrintWhenExpression(expression(
                    "$F{rowType} != null && (" +
                            "\"SECTION\".equalsIgnoreCase(String.valueOf($F{rowType})) || " +
                            "\"SUBSECTION\".equalsIgnoreCase(String.valueOf($F{rowType})) || " +
                            "\"TOTAL\".equalsIgnoreCase(String.valueOf($F{rowType})))"
            ));

            JRDesignTextField normalField = textField(
                    "$F{column" + (index + 1) + "}",
                    x,
                    0,
                    widths[index],
                    DETAIL_ROW_HEIGHT,
                    index == 0 ? textStyle : numericStyle
            );
            normalField.setBlankWhenNull(true);
            normalField.setPrintWhenExpression(expression(
                    "$F{rowType} == null || (" +
                            "!\"SECTION\".equalsIgnoreCase(String.valueOf($F{rowType})) && " +
                            "!\"SUBSECTION\".equalsIgnoreCase(String.valueOf($F{rowType})) && " +
                            "!\"TOTAL\".equalsIgnoreCase(String.valueOf($F{rowType})))"
            ));

            applyThinBorder(emphasizedField);
            applyThinBorder(normalField);
            band.addElement(emphasizedField);
            band.addElement(normalField);
            x += widths[index];
        }

        return band;
    }

    private JRDesignStyle createHeaderStyle(
            JasperDesign design,
            String fontName,
            float fontSize,
            Color backgroundColor,
            Color textColor
    ) throws JRException {
        JRDesignStyle style = new JRDesignStyle();
        style.setName("HeaderStyle");
        style.setFontName(fontName);
        style.setFontSize(fontSize);
        style.setBold(true);
        style.setMode(ModeEnum.OPAQUE);
        style.setBackcolor(backgroundColor);
        style.setForecolor(textColor);
        style.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        style.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        design.addStyle(style);
        return style;
    }

    private JRDesignStyle createTextStyle(
            JasperDesign design,
            String name,
            String fontName,
            float fontSize,
            HorizontalTextAlignEnum alignment
    ) throws JRException {
        JRDesignStyle style = new JRDesignStyle();
        style.setName(name);
        style.setFontName(fontName);
        style.setFontSize(fontSize);
        style.setHorizontalTextAlign(alignment);
        style.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        design.addStyle(style);
        return style;
    }

    private JRDesignTextField textField(
            String expressionText,
            int x,
            int y,
            int width,
            int height,
            JRDesignStyle style
    ) throws JRException {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(x);
        field.setY(y);
        field.setWidth(width);
        field.setHeight(height);
        field.setStyle(style);
        field.setExpression(expression(expressionText));
        return field;
    }

    private JRDesignExpression expression(String text) {
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(text);
        return expression;
    }

    private void addField(JasperDesign design, String name) throws JRException {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClass(String.class);
        design.addField(field);
    }

    private void addParameter(JasperDesign design, String name, Class<?> valueClass) throws JRException {
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setName(name);
        parameter.setValueClass(valueClass);
        design.addParameter(parameter);
    }

    private void applyBaseTemplateStyle(
            JasperDesign design,
            String styleName,
            String fontName,
            float fontSize,
            HorizontalTextAlignEnum alignment,
            Color textColor
    ) {
        JRStyle style = design.getStylesList().stream()
                .filter(candidate -> styleName.equals(candidate.getName()))
                .findFirst()
                .orElse(null);

        if (!(style instanceof JRDesignStyle designStyle)) {
            return;
        }

        designStyle.setFontName(fontName);
        designStyle.setFontSize(fontSize);
        designStyle.setHorizontalTextAlign(alignment);
        if (textColor != null) {
            designStyle.setForecolor(textColor);
        }
    }

    private int[] resolveColumnWidths(int columnCount, int totalWidth) {
        if (columnCount <= 0) {
            return new int[]{totalWidth};
        }
        if (columnCount == 1) {
            return new int[]{totalWidth};
        }

        int minOtherWidth = 70;
        int desiredFirstWidth = Math.round(totalWidth * 0.34f);
        int maxFirstWidth = totalWidth - ((columnCount - 1) * minOtherWidth);
        int firstWidth = Math.max(160, Math.min(desiredFirstWidth, Math.max(160, maxFirstWidth)));
        int remainingWidth = totalWidth - firstWidth;
        int baseOtherWidth = remainingWidth / (columnCount - 1);
        int remainder = remainingWidth % (columnCount - 1);

        int[] widths = new int[columnCount];
        widths[0] = firstWidth;
        for (int index = 1; index < columnCount; index++) {
            widths[index] = baseOtherWidth + (index <= remainder ? 1 : 0);
        }
        return widths;
    }

    private void applyThinBorder(JRDesignTextField field) {
        field.getLineBox().getPen().setLineWidth(0.5f);
    }

    private void applyThinBorder(JRDesignStaticText text) {
        text.getLineBox().getPen().setLineWidth(0.5f);
    }

    private JRDesignBand createSummaryBand(
            JasperDesign design,
            List<FinancialStatementSignatureBlock> signatures,
            JRDesignStyle textStyle
    ) throws JRException {
        JRDesignBand band = new JRDesignBand();
        List<FinancialStatementSignatureBlock> availableSignatures = signatures != null
                ? signatures.stream()
                .filter(signature -> signature != null && signature.image() != null && signature.image().length > 0)
                .limit(2)
                .toList()
                : List.of();
        if (availableSignatures.isEmpty()) {
            band.setHeight(0);
            return band;
        }

        band.setHeight(160);
        band.setSplitType(SplitTypeEnum.STRETCH);

        int blockWidth = 240;
        int gap = 40;
        int startX = availableSignatures.size() == 1
                ? (DESIGN_WIDTH - blockWidth) / 2
                : (DESIGN_WIDTH - ((blockWidth * 2) + gap)) / 2;

        addSignatureBlock(band, design, textStyle, 1, startX, blockWidth);
        if (availableSignatures.size() > 1) {
            addSignatureBlock(band, design, textStyle, 2, startX + blockWidth + gap, blockWidth);
        }

        return band;
    }

    private void addSignatureBlock(
            JRDesignBand band,
            JasperDesign design,
            JRDesignStyle textStyle,
            int signatureIndex,
            int x,
            int blockWidth
    ) throws JRException {
        JRDesignImage image = new JRDesignImage(design);
        image.setX(x + ((blockWidth - 180) / 2));
        image.setY(10);
        image.setWidth(180);
        image.setHeight(70);
        image.setUsingCache(false);
        image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
        image.setExpression(expression("$P{SIGNATURE_IMAGE_" + signatureIndex + "}"));
        band.addElement(image);

        JRDesignTextField nameField = textField(
                "$P{SIGNATURE_NAME_" + signatureIndex + "}",
                x,
                95,
                blockWidth,
                18,
                textStyle
        );
        nameField.setBlankWhenNull(true);
        nameField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        band.addElement(nameField);

        JRDesignTextField roleField = textField(
                "$P{SIGNATURE_ROLE_" + signatureIndex + "}",
                x,
                117,
                blockWidth,
                18,
                textStyle
        );
        roleField.setBlankWhenNull(true);
        roleField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        band.addElement(roleField);
    }
}
