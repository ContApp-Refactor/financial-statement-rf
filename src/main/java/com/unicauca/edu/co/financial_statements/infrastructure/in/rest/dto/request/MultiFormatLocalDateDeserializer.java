package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public class MultiFormatLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    );

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String rawValue = parser.getValueAsString();
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        String normalized = rawValue.trim();
        for (DateTimeFormatter formatter : SUPPORTED_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        throw context.weirdStringException(
                normalized,
                LocalDate.class,
                "Formato de fecha no soportado. Use yyyy-MM-dd o dd/MM/yyyy."
        );
    }
}
