package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@Component
public class FinancialPositionAmountCalculator {

    private final FinancialPositionEntryClassifier classifier;
    private final AccountingEntryOperations accountingEntryOperations;
    private final PeriodResultCalculator periodResultCalculator;

    public FinancialPositionAmountCalculator(
            FinancialPositionEntryClassifier classifier,
            AccountingEntryOperations accountingEntryOperations,
            PeriodResultCalculator periodResultCalculator
    ) {
        this.classifier = classifier;
        this.accountingEntryOperations = accountingEntryOperations;
        this.periodResultCalculator = periodResultCalculator;
    }

    public FinancialPositionAmounts calculate(
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            LocalDate currentCutoffDate,
            LocalDate previousCutoffDate
    ) {
        List<AccountingEntry> safeCurrentEntries = currentEntries != null ? currentEntries : List.of();
        List<AccountingEntry> safePreviousEntries = previousEntries != null ? previousEntries : List.of();

        ComparativeAmount efectivo = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isCashAssetEntry);
        ComparativeAmount deudores = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isReceivableAssetEntry);
        ComparativeAmount activosFinancierosTemporales = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isTemporaryFinancialAssetEntry);
        ComparativeAmount inventarios = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isInventoryAssetEntry);
        ComparativeAmount impuestosCorrientes = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isCurrentTaxAssetEntry);
        ComparativeAmount activosBiologicos = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isBiologicalAssetEntry);
        ComparativeAmount activosMantenidosVenta = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isHeldForSaleAssetEntry);
        ComparativeAmount propiedadPlantaEquipo = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isPropertyPlantEquipmentEntry);
        ComparativeAmount activosFinancierosPermanentes = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isPermanentFinancialAssetEntry);
        ComparativeAmount intangibles = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isIntangibleAssetEntry);
        ComparativeAmount propiedadesInversion = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isInvestmentPropertyEntry);
        ComparativeAmount otrosActivos = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isOtherAssetEntry);
        ComparativeAmount acreedores = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isTradePayableEntry);
        ComparativeAmount pasivosFinancieros = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isCurrentFinancialLiability);
        ComparativeAmount pasivosImpuestosCorrientes = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isCurrentTaxLiabilityEntry);
        ComparativeAmount provision = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isProvisionLiabilityEntry);
        ComparativeAmount pasivosFinancierosLargoPlazo = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isLongTermFinancialLiabilityEntry);
        ComparativeAmount pasivosImpuestosDiferidos = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isDeferredTaxLiabilityEntry);
        ComparativeAmount capitalSuscrito = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isCapitalEntry);
        ComparativeAmount reservas = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isReserveEntry);
        ComparativeAmount utilidadesAcumuladas = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isRetainedEarningsEntry);
        ComparativeAmount utilidadesEjercicio = comparative(
                periodResultCalculator.resolveResultForCutoff(safeCurrentEntries, currentCutoffDate),
                periodResultCalculator.resolveResultForCutoff(safePreviousEntries, previousCutoffDate)
        );
        ComparativeAmount dividendosDecretados = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isDividendEntry);
        ComparativeAmount accionesPropias = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isTreasuryShareEntry);
        ComparativeAmount primaEmision = sumByMatcher(safeCurrentEntries, safePreviousEntries, classifier::isSharePremiumEntry);

        ComparativeAmount totalActivoCorriente = comparative(
                efectivo.current()
                        .add(deudores.current())
                        .add(activosFinancierosTemporales.current())
                        .add(inventarios.current())
                        .add(impuestosCorrientes.current())
                        .add(activosBiologicos.current())
                        .add(activosMantenidosVenta.current()),
                efectivo.previous()
                        .add(deudores.previous())
                        .add(activosFinancierosTemporales.previous())
                        .add(inventarios.previous())
                        .add(impuestosCorrientes.previous())
                        .add(activosBiologicos.previous())
                        .add(activosMantenidosVenta.previous())
        );

        ComparativeAmount totalActivoNoCorriente = comparative(
                propiedadPlantaEquipo.current()
                        .add(activosFinancierosPermanentes.current())
                        .add(intangibles.current())
                        .add(propiedadesInversion.current())
                        .add(otrosActivos.current()),
                propiedadPlantaEquipo.previous()
                        .add(activosFinancierosPermanentes.previous())
                        .add(intangibles.previous())
                        .add(propiedadesInversion.previous())
                        .add(otrosActivos.previous())
        );

        ComparativeAmount totalActivos = comparative(
                totalActivoCorriente.current().add(totalActivoNoCorriente.current()),
                totalActivoCorriente.previous().add(totalActivoNoCorriente.previous())
        );

        ComparativeAmount totalPasivoCorriente = comparative(
                acreedores.current()
                        .add(pasivosFinancieros.current())
                        .add(pasivosImpuestosCorrientes.current())
                        .add(provision.current()),
                acreedores.previous()
                        .add(pasivosFinancieros.previous())
                        .add(pasivosImpuestosCorrientes.previous())
                        .add(provision.previous())
        );

        ComparativeAmount totalPasivoNoCorriente = comparative(
                pasivosFinancierosLargoPlazo.current().add(pasivosImpuestosDiferidos.current()),
                pasivosFinancierosLargoPlazo.previous().add(pasivosImpuestosDiferidos.previous())
        );

        ComparativeAmount totalPasivos = comparative(
                totalPasivoCorriente.current().add(totalPasivoNoCorriente.current()),
                totalPasivoCorriente.previous().add(totalPasivoNoCorriente.previous())
        );

        ComparativeAmount totalPatrimonio = comparative(
                capitalSuscrito.current()
                        .add(reservas.current())
                        .add(utilidadesAcumuladas.current())
                        .add(utilidadesEjercicio.current())
                        .subtract(dividendosDecretados.current())
                        .subtract(accionesPropias.current())
                        .add(primaEmision.current()),
                capitalSuscrito.previous()
                        .add(reservas.previous())
                        .add(utilidadesAcumuladas.previous())
                        .add(utilidadesEjercicio.previous())
                        .subtract(dividendosDecretados.previous())
                        .subtract(accionesPropias.previous())
                        .add(primaEmision.previous())
        );

        return new FinancialPositionAmounts(
                efectivo,
                deudores,
                activosFinancierosTemporales,
                inventarios,
                impuestosCorrientes,
                activosBiologicos,
                activosMantenidosVenta,
                propiedadPlantaEquipo,
                activosFinancierosPermanentes,
                intangibles,
                propiedadesInversion,
                otrosActivos,
                acreedores,
                pasivosFinancieros,
                pasivosImpuestosCorrientes,
                provision,
                pasivosFinancierosLargoPlazo,
                pasivosImpuestosDiferidos,
                capitalSuscrito,
                reservas,
                utilidadesAcumuladas,
                utilidadesEjercicio,
                dividendosDecretados,
                accionesPropias,
                primaEmision,
                totalActivoCorriente,
                totalActivoNoCorriente,
                totalActivos,
                totalPasivoCorriente,
                totalPasivoNoCorriente,
                totalPasivos,
                totalPatrimonio
        );
    }

    private ComparativeAmount sumByMatcher(
            List<AccountingEntry> currentEntries,
            List<AccountingEntry> previousEntries,
            Predicate<AccountingEntry> matcher
    ) {
        return comparative(
                sumEntries(currentEntries, matcher),
                sumEntries(previousEntries, matcher)
        );
    }

    private BigDecimal sumEntries(List<AccountingEntry> entries, Predicate<AccountingEntry> matcher) {
        if (entries == null || entries.isEmpty() || matcher == null) {
            return scaleAmount(BigDecimal.ZERO);
        }

        return scaleAmount(entries.stream()
                .filter(entry -> entry != null && matcher.test(entry))
                .map(accountingEntryOperations::signedAmountByNature)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private ComparativeAmount comparative(BigDecimal current, BigDecimal previous) {
        return new ComparativeAmount(scaleAmount(current), scaleAmount(previous));
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
