package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialPositionEntryClassifierTest {

    private final FinancialPositionEntryClassifier classifier =
            new FinancialPositionEntryClassifier(new AccountingEntryOperations());

    @Test
    void shouldClassifyFinancialPositionAccountsPrimarilyByCode() {
        assertThat(classifier.isInvestmentPropertyEntry(debitEntry("151610", "Activo fijo diverso"))).isTrue();
        assertThat(classifier.isPropertyPlantEquipmentEntry(debitEntry("151610", "Activo fijo diverso"))).isFalse();

        assertThat(classifier.isBiologicalAssetEntry(debitEntry("146505", "Inventario especial"))).isTrue();
        assertThat(classifier.isInventoryAssetEntry(debitEntry("146505", "Inventario especial"))).isFalse();

        assertThat(classifier.isSharePremiumEntry(creditEntry("320510", "Superavit especial"))).isTrue();
        assertThat(classifier.isTreasuryShareEntry(debitEntry("320505", "Cuenta patrimonial"))).isTrue();
        assertThat(classifier.isDividendEntry(debitEntry("370505", "Distribucion aprobada"))).isTrue();
    }

    @Test
    void shouldUseNameFallbackOnlyForAmbiguousMockLiabilityCodes() {
        AccountingEntry providerLiability = creditEntry("210505", "Pasivo corriente - Proveedores");
        AccountingEntry longTermFinancialLiability = creditEntry("220505", "Pasivo no corriente - Obligaciones financieras");
        AccountingEntry genericGroup22Liability = creditEntry("220505", "Cuenta 220505");

        assertThat(classifier.isTradePayableEntry(providerLiability)).isTrue();
        assertThat(classifier.isCurrentFinancialLiability(providerLiability)).isFalse();

        assertThat(classifier.isLongTermFinancialLiabilityEntry(longTermFinancialLiability)).isTrue();
        assertThat(classifier.isTradePayableEntry(longTermFinancialLiability)).isFalse();

        assertThat(classifier.isTradePayableEntry(genericGroup22Liability)).isTrue();
        assertThat(classifier.isLongTermFinancialLiabilityEntry(genericGroup22Liability)).isFalse();
    }

    private AccountingEntry debitEntry(String code, String name) {
        return AccountingEntry.builder()
                .accountCode(code)
                .accountName(name)
                .accountNature("DEBITO")
                .debit(BigDecimal.ONE)
                .credit(BigDecimal.ZERO)
                .build();
    }

    private AccountingEntry creditEntry(String code, String name) {
        return AccountingEntry.builder()
                .accountCode(code)
                .accountName(name)
                .accountNature("CREDITO")
                .debit(BigDecimal.ZERO)
                .credit(BigDecimal.ONE)
                .build();
    }
}
