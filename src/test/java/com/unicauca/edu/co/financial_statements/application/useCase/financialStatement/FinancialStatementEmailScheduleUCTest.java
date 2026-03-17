package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementDeliveryPort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementPersistencePort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EFinancialStatementType;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportDeliveryFrequency;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEmailScheduleEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialStatementEmailScheduleUCTest {

    @Mock
    private IFinancialStatementPersistencePort financialStatementPersistencePort;

    @Mock
    private IFinancialStatementCommandPort financialStatementCommandPort;

    @Mock
    private IFinancialStatementDeliveryPort financialStatementDeliveryPort;

    @InjectMocks
    private FinancialStatementEmailScheduleUC useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "defaultTimezone", "America/Bogota");
        ReflectionTestUtils.setField(useCase, "claimLeaseMs", 300_000L);
        ReflectionTestUtils.setField(useCase, "failureRetryDelayMs", 120_000L);
    }

    @Test
    void shouldSkipDueScheduleWhenClaimIsNotAcquired() throws Exception {
        FinancialStatementEmailScheduleEntity dueSchedule = dueSchedule();

        when(financialStatementPersistencePort.findDueActiveEmailSchedules(any(OffsetDateTime.class)))
                .thenReturn(List.of(dueSchedule));
        when(financialStatementPersistencePort.claimDueEmailSchedule(
                eq(dueSchedule.getId()),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(false);

        useCase.processDueSchedules();

        verify(financialStatementDeliveryPort, never()).exportByEmail(any());
        verify(financialStatementPersistencePort, never()).saveEmailSchedule(any());
        verify(financialStatementCommandPort, never()).registerDeliveryEvent(any(), anyString(), anyString(), anyString());
        verify(financialStatementCommandPort, never()).registerLogEvent(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRescheduleClaimedScheduleAfterSuccessfulDelivery() throws Exception {
        FinancialStatementEmailScheduleEntity dueSchedule = dueSchedule();
        FinancialStatementGenerationResult snapshot = snapshotFor(dueSchedule.getFinancialStatement().getReportId());

        when(financialStatementPersistencePort.findDueActiveEmailSchedules(any(OffsetDateTime.class)))
                .thenReturn(List.of(dueSchedule));
        when(financialStatementPersistencePort.claimDueEmailSchedule(
                eq(dueSchedule.getId()),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(true);
        when(financialStatementPersistencePort.findEmailScheduleById(dueSchedule.getId()))
                .thenReturn(Optional.of(dueSchedule));
        when(financialStatementCommandPort.getFinancialStatementSnapshot(dueSchedule.getFinancialStatement().getReportId()))
                .thenReturn(Optional.of(snapshot));
        when(financialStatementCommandPort.getDefaultTemplateByEnterprise("ENT-SCHEDULE-001"))
                .thenReturn(Optional.empty());
        when(financialStatementPersistencePort.saveEmailSchedule(any(FinancialStatementEmailScheduleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.processDueSchedules();

        ArgumentCaptor<FinancialStatementEmailScheduleEntity> savedCaptor =
                ArgumentCaptor.forClass(FinancialStatementEmailScheduleEntity.class);
        verify(financialStatementPersistencePort).saveEmailSchedule(savedCaptor.capture());
        FinancialStatementEmailScheduleEntity saved = savedCaptor.getValue();

        assertThat(saved.getLastRunAt()).isNotNull();
        assertThat(saved.getNextRunAt()).isAfter(saved.getLastRunAt());
        verify(financialStatementDeliveryPort).exportByEmail(any());
        verify(financialStatementCommandPort).registerDeliveryEvent(
                eq(dueSchedule.getFinancialStatement().getReportId()),
                eq("SCHEDULED_EMAIL"),
                anyString(),
                eq("EMAILED")
        );
    }

    @Test
    void shouldScheduleRetryWhenClaimedDeliveryFails() throws Exception {
        FinancialStatementEmailScheduleEntity dueSchedule = dueSchedule();
        FinancialStatementGenerationResult snapshot = snapshotFor(dueSchedule.getFinancialStatement().getReportId());

        when(financialStatementPersistencePort.findDueActiveEmailSchedules(any(OffsetDateTime.class)))
                .thenReturn(List.of(dueSchedule));
        when(financialStatementPersistencePort.claimDueEmailSchedule(
                eq(dueSchedule.getId()),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(true);
        when(financialStatementPersistencePort.findEmailScheduleById(dueSchedule.getId()))
                .thenReturn(Optional.of(dueSchedule));
        when(financialStatementCommandPort.getFinancialStatementSnapshot(dueSchedule.getFinancialStatement().getReportId()))
                .thenReturn(Optional.of(snapshot));
        when(financialStatementCommandPort.getDefaultTemplateByEnterprise("ENT-SCHEDULE-001"))
                .thenReturn(Optional.empty());
        when(financialStatementPersistencePort.saveEmailSchedule(any(FinancialStatementEmailScheduleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP temporalmente no disponible"))
                .when(financialStatementDeliveryPort)
                .exportByEmail(any());

        useCase.processDueSchedules();

        ArgumentCaptor<FinancialStatementEmailScheduleEntity> savedCaptor =
                ArgumentCaptor.forClass(FinancialStatementEmailScheduleEntity.class);
        verify(financialStatementPersistencePort).saveEmailSchedule(savedCaptor.capture());
        FinancialStatementEmailScheduleEntity saved = savedCaptor.getValue();

        assertThat(saved.getNextRunAt()).isAfter(saved.getUpdatedAt());
        verify(financialStatementCommandPort).registerLogEvent(
                eq(dueSchedule.getFinancialStatement().getReportId()),
                eq("EMAIL_FAILED"),
                anyString(),
                eq("error"),
                eq("ERROR")
        );
        verify(financialStatementCommandPort, never()).registerDeliveryEvent(any(), anyString(), anyString(), anyString());
    }

    private FinancialStatementEmailScheduleEntity dueSchedule() {
        OffsetDateTime now = OffsetDateTime.now().minusMinutes(2);
        UUID reportId = UUID.randomUUID();
        FinancialStatementEntity statement = FinancialStatementEntity.builder()
                .id(10L)
                .reportId(reportId)
                .type(EFinancialStatementType.INCOME_STATEMENT)
                .entId("ENT-SCHEDULE-001")
                .createdAt(now.minusDays(1))
                .reportSnapshot("{\"version\":1}")
                .build();

        return FinancialStatementEmailScheduleEntity.builder()
                .id(20L)
                .financialStatement(statement)
                .recipientEmail("usuario@dominio.com")
                .format(EReportExportFormat.PDF)
                .frequency(EReportDeliveryFrequency.DAILY)
                .hourOfDay(8)
                .minuteOfHour(30)
                .timezone("America/Bogota")
                .active(Boolean.TRUE)
                .nextRunAt(now)
                .lastRunAt(null)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusHours(1))
                .build();
    }

    private FinancialStatementGenerationResult snapshotFor(UUID reportId) {
        return FinancialStatementGenerationResult.builder()
                .financialStatement(FinancialStatementReport.builder()
                        .reportId(reportId)
                        .type(EFinancialStatementType.INCOME_STATEMENT)
                        .entId("ENT-SCHEDULE-001")
                        .createdAt(OffsetDateTime.now().minusDays(1))
                        .build())
                .financialStatementData(List.of())
                .build();
    }
}
