package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementCriteriaType;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingEntryOperationsTest {

    private final AccountingEntryOperations operations = new AccountingEntryOperations();

    @Test
    void shouldResolveAccountCodeAndPrefixChecks() {
        AccountingEntry entry = AccountingEntry.builder()
                .accountCode("413505")
                .build();

        assertThat(operations.resolveAccountCode(entry)).isEqualTo("413505");
        assertThat(operations.codeStartsWith(entry, "41")).isTrue();
        assertThat(operations.isIncomeStatementAccount(entry)).isTrue();
    }

    @Test
    void shouldProjectAccountCodeAndMatchRange() {
        AccountingEntry entry = AccountingEntry.builder()
                .accountCode("135515")
                .build();

        assertThat(operations.resolveProjectedAccountCode(entry, 4)).isEqualTo("1355");
        assertThat(operations.matchesCriteriaRange(entry, 4, 1300, 1399)).isTrue();
        assertThat(operations.matchesCriteriaRange(entry, 2, 21, 29)).isFalse();
    }

    @Test
    void shouldComputeSortableAccountCodeSafely() {
        assertThat(operations.parseSortableAccountCode("2408")).isEqualTo(2408L);
        assertThat(operations.parseSortableAccountCode("ABC")).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldCalculateSignedAmountByNature() {
        AccountingEntry credito = AccountingEntry.builder()
                .accountNature("credito")
                .debit(new BigDecimal("10"))
                .credit(new BigDecimal("35"))
                .build();
        AccountingEntry debito = AccountingEntry.builder()
                .accountNature("debito")
                .debit(new BigDecimal("50"))
                .credit(new BigDecimal("5"))
                .build();

        assertThat(operations.signedAmountByNature(credito)).isEqualByComparingTo("25");
        assertThat(operations.signedAmountByNature(debito)).isEqualByComparingTo("45");
        assertThat(operations.normalizeNature(null)).isEqualTo("DEBITO");
    }

    @Test
    void shouldResolveCriteriaTypeMetadata() {
        assertThat(EFinancialStatementCriteriaType.resolvePrefixLength("SUB_ACCOUNT")).isEqualTo(6);
        assertThat(EFinancialStatementCriteriaType.resolveLabel("AUXILIARY_ACCOUNT")).isEqualTo("Auxiliar");
        assertThat(EFinancialStatementCriteriaType.resolvePrefixLength("UNKNOWN")).isEqualTo(0);
    }
}
