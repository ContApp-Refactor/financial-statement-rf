package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementCriteria;
import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FinancialPositionStatementBuilder {

    private final FinancialPositionAmountCalculator financialPositionAmountCalculator;
    private final FinancialPositionEntryClassifier financialPositionEntryClassifier;
    private final FinancialStatementComparativeRowBuilder financialStatementComparativeRowBuilder;
    private final AccountingEntryOperations accountingEntryOperations;

    public FinancialPositionStatementBuilder(
            FinancialPositionAmountCalculator financialPositionAmountCalculator,
            FinancialPositionEntryClassifier financialPositionEntryClassifier,
            FinancialStatementComparativeRowBuilder financialStatementComparativeRowBuilder,
            AccountingEntryOperations accountingEntryOperations
    ) {
        this.financialPositionAmountCalculator = financialPositionAmountCalculator;
        this.financialPositionEntryClassifier = financialPositionEntryClassifier;
        this.financialStatementComparativeRowBuilder = financialStatementComparativeRowBuilder;
        this.accountingEntryOperations = accountingEntryOperations;
    }

    public FinancialPositionRowsResult build(
            List<AccountingEntry> accountingEntries,
            List<AccountingEntry> previousAccountingEntries,
            FinancialStatementCriteria criteria,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        List<AccountingEntry> currentEntries = accountingEntries != null ? accountingEntries : List.of();
        List<AccountingEntry> previousEntries = previousAccountingEntries != null ? previousAccountingEntries : List.of();
        FinancialPositionAmounts amounts = financialPositionAmountCalculator.calculate(
                currentEntries,
                previousEntries,
                currentCutoffDate,
                previousCutoffDate
        );

        BigDecimal currentEfectivo = amounts.efectivo().current();
        BigDecimal previousEfectivo = amounts.efectivo().previous();
        BigDecimal currentDeudores = amounts.deudores().current();
        BigDecimal previousDeudores = amounts.deudores().previous();
        BigDecimal currentActivosFinancierosTemporales = amounts.activosFinancierosTemporales().current();
        BigDecimal previousActivosFinancierosTemporales = amounts.activosFinancierosTemporales().previous();
        BigDecimal currentInventarios = amounts.inventarios().current();
        BigDecimal previousInventarios = amounts.inventarios().previous();
        BigDecimal currentImpuestosCorrientes = amounts.impuestosCorrientes().current();
        BigDecimal previousImpuestosCorrientes = amounts.impuestosCorrientes().previous();
        BigDecimal currentActivosBiologicos = amounts.activosBiologicos().current();
        BigDecimal previousActivosBiologicos = amounts.activosBiologicos().previous();
        BigDecimal currentActivosMantenidosVenta = amounts.activosMantenidosVenta().current();
        BigDecimal previousActivosMantenidosVenta = amounts.activosMantenidosVenta().previous();
        BigDecimal currentPpe = amounts.propiedadPlantaEquipo().current();
        BigDecimal previousPpe = amounts.propiedadPlantaEquipo().previous();
        BigDecimal currentActivosFinancierosPermanentes = amounts.activosFinancierosPermanentes().current();
        BigDecimal previousActivosFinancierosPermanentes = amounts.activosFinancierosPermanentes().previous();
        BigDecimal currentIntangibles = amounts.intangibles().current();
        BigDecimal previousIntangibles = amounts.intangibles().previous();
        BigDecimal currentPropiedadesInversion = amounts.propiedadesInversion().current();
        BigDecimal previousPropiedadesInversion = amounts.propiedadesInversion().previous();
        BigDecimal currentOtrosActivos = amounts.otrosActivos().current();
        BigDecimal previousOtrosActivos = amounts.otrosActivos().previous();
        BigDecimal currentAcreedores = amounts.acreedores().current();
        BigDecimal previousAcreedores = amounts.acreedores().previous();
        BigDecimal currentPasivosFinancieros = amounts.pasivosFinancieros().current();
        BigDecimal previousPasivosFinancieros = amounts.pasivosFinancieros().previous();
        BigDecimal currentPasivosImpuestosCorrientes = amounts.pasivosImpuestosCorrientes().current();
        BigDecimal previousPasivosImpuestosCorrientes = amounts.pasivosImpuestosCorrientes().previous();
        BigDecimal currentProvision = amounts.provision().current();
        BigDecimal previousProvision = amounts.provision().previous();
        BigDecimal currentPasivosFinancierosLargoPlazo = amounts.pasivosFinancierosLargoPlazo().current();
        BigDecimal previousPasivosFinancierosLargoPlazo = amounts.pasivosFinancierosLargoPlazo().previous();
        BigDecimal currentPasivosImpuestosDiferidos = amounts.pasivosImpuestosDiferidos().current();
        BigDecimal previousPasivosImpuestosDiferidos = amounts.pasivosImpuestosDiferidos().previous();
        BigDecimal currentCapitalSuscrito = amounts.capitalSuscrito().current();
        BigDecimal previousCapitalSuscrito = amounts.capitalSuscrito().previous();
        BigDecimal currentReservas = amounts.reservas().current();
        BigDecimal previousReservas = amounts.reservas().previous();
        BigDecimal currentUtilidadesAcumuladas = amounts.utilidadesAcumuladas().current();
        BigDecimal previousUtilidadesAcumuladas = amounts.utilidadesAcumuladas().previous();
        BigDecimal currentUtilidadesEjercicio = amounts.utilidadesEjercicio().current();
        BigDecimal previousUtilidadesEjercicio = amounts.utilidadesEjercicio().previous();
        BigDecimal currentDividendosDecretados = amounts.dividendosDecretados().current();
        BigDecimal previousDividendosDecretados = amounts.dividendosDecretados().previous();
        BigDecimal currentAccionesPropias = amounts.accionesPropias().current();
        BigDecimal previousAccionesPropias = amounts.accionesPropias().previous();
        BigDecimal currentPrimaEmision = amounts.primaEmision().current();
        BigDecimal previousPrimaEmision = amounts.primaEmision().previous();
        BigDecimal totalActivoCorriente = amounts.totalActivoCorriente().current();
        BigDecimal previousTotalActivoCorriente = amounts.totalActivoCorriente().previous();
        BigDecimal totalActivoNoCorriente = amounts.totalActivoNoCorriente().current();
        BigDecimal previousTotalActivoNoCorriente = amounts.totalActivoNoCorriente().previous();
        BigDecimal totalAssets = amounts.totalActivos().current();
        BigDecimal previousTotalAssets = amounts.totalActivos().previous();
        BigDecimal totalPasivoCorriente = amounts.totalPasivoCorriente().current();
        BigDecimal previousTotalPasivoCorriente = amounts.totalPasivoCorriente().previous();
        BigDecimal totalPasivoNoCorriente = amounts.totalPasivoNoCorriente().current();
        BigDecimal previousTotalPasivoNoCorriente = amounts.totalPasivoNoCorriente().previous();
        BigDecimal totalLiabilities = amounts.totalPasivos().current();
        BigDecimal previousTotalLiabilities = amounts.totalPasivos().previous();
        BigDecimal totalEquity = amounts.totalPatrimonio().current();
        BigDecimal previousTotalEquity = amounts.totalPatrimonio().previous();

        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildFinancialPositionRow("ACTIVO", null, null, null, "SECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("ACTIVO CORRIENTE", null, null, null, "SUBSECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Efectivo y equivalente al efectivo", null, scaleAmount(currentEfectivo), scaleAmount(previousEfectivo), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isCashAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Deudores comerciales y otras cuentas x cobrar", null, scaleAmount(currentDeudores), scaleAmount(previousDeudores), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isReceivableAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos financieros (inversion temporal)", null, scaleAmount(currentActivosFinancierosTemporales), scaleAmount(previousActivosFinancierosTemporales), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isTemporaryFinancialAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Inventarios", null, scaleAmount(currentInventarios), scaleAmount(previousInventarios), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isInventoryAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos por impuestos corrientes", null, scaleAmount(currentImpuestosCorrientes), scaleAmount(previousImpuestosCorrientes), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isCurrentTaxAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos biologicos", null, scaleAmount(currentActivosBiologicos), scaleAmount(previousActivosBiologicos), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isBiologicalAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos mantenidos para la venta", null, scaleAmount(currentActivosMantenidosVenta), scaleAmount(previousActivosMantenidosVenta), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isHeldForSaleAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("TOTAL ACTIVO CORRIENTE", null, totalActivoCorriente, previousTotalActivoCorriente, "TOTAL", totalAssets, previousTotalAssets));

        rows.add(buildFinancialPositionRow("ACTIVO NO CORRIENTE", null, null, null, "SUBSECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Propiedad, planta y equipo", null, scaleAmount(currentPpe), scaleAmount(previousPpe), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isPropertyPlantEquipmentEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos financieros (inversiones permanentes)", null, scaleAmount(currentActivosFinancierosPermanentes), scaleAmount(previousActivosFinancierosPermanentes), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isPermanentFinancialAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Activos intangibles", null, scaleAmount(currentIntangibles), scaleAmount(previousIntangibles), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isIntangibleAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Propiedades de inversion", null, scaleAmount(currentPropiedadesInversion), scaleAmount(previousPropiedadesInversion), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isInvestmentPropertyEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Otros activos", null, scaleAmount(currentOtrosActivos), scaleAmount(previousOtrosActivos), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isOtherAssetEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("TOTAL ACTIVO NO CORRIENTE", null, totalActivoNoCorriente, previousTotalActivoNoCorriente, "TOTAL", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("TOTAL ACTIVOS", null, totalAssets, previousTotalAssets, "TOTAL", totalAssets, previousTotalAssets));

        rows.add(buildFinancialPositionRow("PASIVOS", null, null, null, "SECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("PASIVO CORRIENTE", null, null, null, "SUBSECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Acreedores comerciales y otras cuentas por pagar", null, scaleAmount(currentAcreedores), scaleAmount(previousAcreedores), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isTradePayableEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Pasivos financieros", null, scaleAmount(currentPasivosFinancieros), scaleAmount(previousPasivosFinancieros), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isCurrentFinancialLiability,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Pasivos por impuestos corrientes", null, scaleAmount(currentPasivosImpuestosCorrientes), scaleAmount(previousPasivosImpuestosCorrientes), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isCurrentTaxLiabilityEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Provision", null, scaleAmount(currentProvision), scaleAmount(previousProvision), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isProvisionLiabilityEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("TOTAL PASIVO CORRIENTE", null, totalPasivoCorriente, previousTotalPasivoCorriente, "TOTAL", totalAssets, previousTotalAssets));

        rows.add(buildFinancialPositionRow("PASIVO NO CORRIENTE", null, null, null, "SUBSECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Pasivos financieros largo plazo", null, scaleAmount(currentPasivosFinancierosLargoPlazo), scaleAmount(previousPasivosFinancierosLargoPlazo), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isLongTermFinancialLiabilityEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Pasivos por impuestos diferidos", null, scaleAmount(currentPasivosImpuestosDiferidos), scaleAmount(previousPasivosImpuestosDiferidos), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isDeferredTaxLiabilityEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("TOTAL PASIVO NO CORRIENTE", null, totalPasivoNoCorriente, previousTotalPasivoNoCorriente, "TOTAL", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("TOTAL PASIVOS", null, totalLiabilities, previousTotalLiabilities, "TOTAL", totalAssets, previousTotalAssets));

        rows.add(buildFinancialPositionRow("PATRIMONIO", null, null, null, "SECTION", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Capital suscrito y pagado", null, scaleAmount(currentCapitalSuscrito), scaleAmount(previousCapitalSuscrito), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isCapitalEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Reservas", null, scaleAmount(currentReservas), scaleAmount(previousReservas), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isReserveEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Utilidades acumuladas", null, scaleAmount(currentUtilidadesAcumuladas), scaleAmount(previousUtilidadesAcumuladas), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isRetainedEarningsEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Utilidades del ejercicio", null, scaleAmount(currentUtilidadesEjercicio), scaleAmount(previousUtilidadesEjercicio), "DETAIL", totalAssets, previousTotalAssets));
        rows.add(buildFinancialPositionRow("Dividendos decretados", null, scaleAmount(currentDividendosDecretados), scaleAmount(previousDividendosDecretados), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isDividendEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Acciones propias readquiridas", null, scaleAmount(currentAccionesPropias), scaleAmount(previousAccionesPropias), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isTreasuryShareEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("Prima de emision", null, scaleAmount(currentPrimaEmision), scaleAmount(previousPrimaEmision), "DETAIL", totalAssets, previousTotalAssets));
        rows.addAll(buildFinancialPositionLevelRows(
                currentEntries,
                previousEntries,
                criteria,
                financialPositionEntryClassifier::isSharePremiumEntry,
                totalAssets,
                previousTotalAssets
        ));
        rows.add(buildFinancialPositionRow("TOTAL PATRIMONIO", null, totalEquity, previousTotalEquity, "TOTAL", totalAssets, previousTotalAssets));

        rows.add(buildFinancialPositionRow(
                "TOTAL PASIVO + PATRIMONIO",
                null,
                scaleAmount(totalLiabilities.add(totalEquity)),
                scaleAmount(previousTotalLiabilities.add(previousTotalEquity)),
                "TOTAL",
                totalAssets,
                previousTotalAssets
        ));

        return new FinancialPositionRowsResult(
                rows,
                totalAssets,
                previousTotalAssets,
                totalLiabilities,
                previousTotalLiabilities,
                totalEquity,
                previousTotalEquity
        );
    }

    private List<Map<String, Object>> buildFinancialPositionLevelRows(
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            FinancialStatementCriteria criteria,
            java.util.function.Predicate<AccountingEntry> matcher,
            BigDecimal totalAssets,
            BigDecimal previousTotalAssets
    ) {
        return financialStatementComparativeRowBuilder.buildLevelComparisonRows(
                currentEntries,
                previousEntries,
                criteria,
                matcher,
                totalAssets,
                previousTotalAssets,
                accountingEntryOperations::signedAmountByNature,
                true
        );
    }

    private Map<String, Object> buildFinancialPositionRow(
            String lineDescription,
            String note,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            String rowType,
            BigDecimal totalAssets,
            BigDecimal previousTotalAssets
    ) {
        return financialStatementComparativeRowBuilder.buildComparativeRow(
                lineDescription,
                note,
                currentAmount,
                previousAmount,
                rowType,
                totalAssets,
                previousTotalAssets
        );
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
