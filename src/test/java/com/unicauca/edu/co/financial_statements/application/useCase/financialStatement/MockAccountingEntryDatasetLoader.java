package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class MockAccountingEntryDatasetLoader {

    private static final DateTimeFormatter MOCK_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path MOCK_DATASET_PATH = Path.of("mock", "mock-income-statement-account-info.json");

    private MockAccountingEntryDatasetLoader() {
    }

    static List<AccountingEntry> load() throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(MOCK_DATASET_PATH));
        JsonNode accountInfo = root.path("accountInfo");

        List<AccountingEntry> entries = new ArrayList<>();
        for (JsonNode node : accountInfo) {
            entries.add(toAccountingEntry(node));
        }
        return entries;
    }

    private static AccountingEntry toAccountingEntry(JsonNode node) {
        return AccountingEntry.builder()
                .entId(node.path("entId").asText())
                .date(LocalDate.parse(node.path("date").asText(), MOCK_DATE_FORMAT))
                .voucherNumber(node.path("voucher").path("number").isMissingNode() ? null : node.path("voucher").path("number").asInt())
                .voucherType(node.path("voucher").path("type").asText(null))
                .accountCode(node.path("account").path("code").asText())
                .accountNature(node.path("account").path("nature").asText())
                .accountName(node.path("account").path("name").asText())
                .thirdPartyId(node.path("thirdPartyId").isMissingNode() ? null : node.path("thirdPartyId").asLong())
                .costCenterCode(node.path("costCenter").path("code").isMissingNode() ? null : node.path("costCenter").path("code").asLong())
                .debit(asBigDecimal(node.path("accountingMovement").path("debit")))
                .credit(asBigDecimal(node.path("accountingMovement").path("credit")))
                .movementDescription(node.path("accountingMovement").path("description").asText(null))
                .build();
    }

    private static BigDecimal asBigDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        return node.decimalValue();
    }
}
