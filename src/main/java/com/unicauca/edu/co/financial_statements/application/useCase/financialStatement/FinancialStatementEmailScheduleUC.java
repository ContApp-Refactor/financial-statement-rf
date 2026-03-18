package com.unicauca.edu.co.financial_statements.application.useCase.financialStatement;

import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementCommandPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementDeliveryPort;
import com.unicauca.edu.co.financial_statements.application.ports.in.financialStatement.IFinancialStatementEmailSchedulePort;
import com.unicauca.edu.co.financial_statements.application.ports.out.IFinancialStatementPersistencePort;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailExportCommand;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementEmailSchedule;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementExportStyle;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementGenerationResult;
import com.unicauca.edu.co.financial_statements.domain.models.core.FinancialStatementReport;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EDeliveryWay;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportDeliveryFrequency;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEmailScheduleEntity;
import com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity.FinancialStatementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FinancialStatementEmailScheduleUC implements IFinancialStatementEmailSchedulePort {

    private static final long MINIMUM_SCHEDULER_DELAY_MS = 1_000L;

    private final IFinancialStatementPersistencePort financialStatementPersistencePort;
    private final IFinancialStatementCommandPort financialStatementCommandPort;
    private final IFinancialStatementDeliveryPort financialStatementDeliveryPort;
    private final FinancialStatementTemplateExportStyleMapper financialStatementTemplateExportStyleMapper;

    @Value("${report-email.scheduler.default-timezone:America/Bogota}")
    private String defaultTimezone;

    @Value("${report-email.scheduler.claim-lease-ms:300000}")
    private long claimLeaseMs;

    @Value("${report-email.scheduler.failure-retry-delay-ms:300000}")
    private long failureRetryDelayMs;

    @Override
    @Transactional
    public FinancialStatementEmailSchedule createSchedule(FinancialStatementEmailSchedule schedule) {
        validateSchedule(schedule);

        FinancialStatementEntity statement = resolveFinancialStatement(schedule.getReportId());
        OffsetDateTime now = OffsetDateTime.now();
        FinancialStatementEmailScheduleEntity entity = FinancialStatementEmailScheduleEntity.builder()
                .financialStatement(statement)
                .recipientEmail(schedule.getRecipientEmail().trim())
                .format(schedule.getFormat() != null ? schedule.getFormat() : EReportExportFormat.PDF)
                .frequency(schedule.getFrequency())
                .hourOfDay(schedule.getHourOfDay())
                .minuteOfHour(schedule.getMinuteOfHour())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfMonth(schedule.getDayOfMonth())
                .timezone(resolveTimezone(schedule.getTimezone()))
                .active(Boolean.TRUE)
                .nextRunAt(calculateNextRunAt(schedule, now))
                .lastRunAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        FinancialStatementEmailSchedule savedEntity = toDomain(
                financialStatementPersistencePort.saveEmailSchedule(entity)
        );

        financialStatementCommandPort.registerLogEvent(
                schedule.getReportId(),
                "SCHEDULE_CREATED",
                "Programacion de correo creada para " + schedule.getRecipientEmail().trim() + ".",
                "schedule",
                "INFO"
        );

        return savedEntity;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialStatementEmailSchedule> getSchedulesByReportId(UUID reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("reportId is required.");
        }

        return financialStatementPersistencePort.findEmailSchedulesByReportId(reportId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public FinancialStatementEmailSchedule updateScheduleStatus(Long scheduleId, boolean active) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("scheduleId is required.");
        }

        FinancialStatementEmailScheduleEntity entity = financialStatementPersistencePort.findEmailScheduleById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Email schedule not found."));

        OffsetDateTime now = OffsetDateTime.now();
        entity.setActive(active);
        entity.setUpdatedAt(now);

        if (active && (entity.getNextRunAt() == null || !entity.getNextRunAt().isAfter(now))) {
            entity.setNextRunAt(calculateNextRunAt(toDomain(entity), now));
        }

        FinancialStatementEmailSchedule savedEntity = toDomain(
                financialStatementPersistencePort.saveEmailSchedule(entity)
        );

        financialStatementCommandPort.registerLogEvent(
                entity.getFinancialStatement().getReportId(),
                "SCHEDULE_UPDATED",
                active ? "Programacion de correo activada." : "Programacion de correo desactivada.",
                "schedule",
                "INFO"
        );

        return savedEntity;
    }

    @Override
    @Scheduled(fixedDelayString = "${report-email.scheduler.fixed-delay-ms:60000}")
    @Transactional
    public void processDueSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        List<FinancialStatementEmailScheduleEntity> dueSchedules = financialStatementPersistencePort
                .findDueActiveEmailSchedules(now);

        for (FinancialStatementEmailScheduleEntity scheduleEntity : dueSchedules) {
            if (scheduleEntity.getId() == null) {
                continue;
            }

            OffsetDateTime claimedUntil = calculateClaimLeaseUntil(now);
            boolean claimed = financialStatementPersistencePort.claimDueEmailSchedule(
                    scheduleEntity.getId(),
                    now,
                    claimedUntil,
                    now
            );
            if (!claimed) {
                continue;
            }

            financialStatementPersistencePort.findEmailScheduleById(scheduleEntity.getId())
                    .ifPresent(claimedSchedule -> processSchedule(claimedSchedule, now));
        }
    }

    private void processSchedule(FinancialStatementEmailScheduleEntity scheduleEntity, OffsetDateTime now) {
        UUID reportId = scheduleEntity.getFinancialStatement().getReportId();

        try {
            FinancialStatementGenerationResult snapshot = financialStatementCommandPort.getFinancialStatementSnapshot(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("Financial statement report not found."));
            FinancialStatementReport report = snapshot.getFinancialStatement();
            FinancialStatementExportStyle exportStyle = financialStatementCommandPort
                    .getDefaultTemplateByEnterprise(report.getEntId())
                    .map(financialStatementTemplateExportStyleMapper::toExportStyle)
                    .orElse(null);

            financialStatementDeliveryPort.exportByEmail(
                    FinancialStatementEmailExportCommand.builder()
                            .reportId(reportId)
                            .format(scheduleEntity.getFormat())
                            .enterpriseName(report.getEntId())
                            .exportStyle(exportStyle)
                            .toEmail(scheduleEntity.getRecipientEmail())
                            .build()
            );

            scheduleEntity.setLastRunAt(now);
            scheduleEntity.setNextRunAt(calculateNextRunAt(toDomain(scheduleEntity), now));
            scheduleEntity.setUpdatedAt(now);
            financialStatementPersistencePort.saveEmailSchedule(scheduleEntity);

            financialStatementCommandPort.registerDeliveryEvent(
                    reportId,
                    EDeliveryWay.SCHEDULED_EMAIL.name(),
                    "Reporte enviado por correo programado a " + scheduleEntity.getRecipientEmail() + ".",
                    "EMAILED"
            );
        } catch (Exception exception) {
            scheduleEntity.setNextRunAt(calculateFailureRetryAt(now));
            scheduleEntity.setUpdatedAt(now);
            financialStatementPersistencePort.saveEmailSchedule(scheduleEntity);

            financialStatementCommandPort.registerLogEvent(
                    reportId,
                    "EMAIL_FAILED",
                    "No fue posible enviar el correo programado: " + resolveExceptionMessage(exception),
                    "error",
                    "ERROR"
            );
        }
    }

    private FinancialStatementEmailSchedule toDomain(FinancialStatementEmailScheduleEntity entity) {
        return FinancialStatementEmailSchedule.builder()
                .id(entity.getId())
                .reportId(entity.getFinancialStatement().getReportId())
                .recipientEmail(entity.getRecipientEmail())
                .format(entity.getFormat())
                .frequency(entity.getFrequency())
                .hourOfDay(entity.getHourOfDay())
                .minuteOfHour(entity.getMinuteOfHour())
                .dayOfWeek(entity.getDayOfWeek())
                .dayOfMonth(entity.getDayOfMonth())
                .timezone(entity.getTimezone())
                .active(entity.getActive())
                .nextRunAt(entity.getNextRunAt())
                .lastRunAt(entity.getLastRunAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateSchedule(FinancialStatementEmailSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule is required.");
        }
        if (schedule.getReportId() == null) {
            throw new IllegalArgumentException("reportId is required.");
        }
        if (!StringUtils.hasText(schedule.getRecipientEmail())) {
            throw new IllegalArgumentException("recipientEmail is required.");
        }
        if (schedule.getFrequency() == null) {
            throw new IllegalArgumentException("frequency is required.");
        }
        if (schedule.getHourOfDay() == null || schedule.getHourOfDay() < 0 || schedule.getHourOfDay() > 23) {
            throw new IllegalArgumentException("hourOfDay must be between 0 and 23.");
        }
        if (schedule.getMinuteOfHour() == null || schedule.getMinuteOfHour() < 0 || schedule.getMinuteOfHour() > 59) {
            throw new IllegalArgumentException("minuteOfHour must be between 0 and 59.");
        }
        if (schedule.getFrequency() == EReportDeliveryFrequency.WEEKLY && schedule.getDayOfWeek() == null) {
            throw new IllegalArgumentException("dayOfWeek is required for weekly schedules.");
        }
        if (schedule.getFrequency() == EReportDeliveryFrequency.MONTHLY && schedule.getDayOfMonth() == null) {
            throw new IllegalArgumentException("dayOfMonth is required for monthly schedules.");
        }
    }

    private OffsetDateTime calculateNextRunAt(FinancialStatementEmailSchedule schedule, OffsetDateTime referenceTime) {
        ZoneId zoneId = ZoneId.of(resolveTimezone(schedule.getTimezone()));
        ZonedDateTime base = referenceTime.atZoneSameInstant(zoneId).withSecond(0).withNano(0);

        return switch (schedule.getFrequency()) {
            case DAILY -> resolveDailyRun(schedule, base).toOffsetDateTime();
            case WEEKLY -> resolveWeeklyRun(schedule, base).toOffsetDateTime();
            case MONTHLY -> resolveMonthlyRun(schedule, base).toOffsetDateTime();
        };
    }

    private ZonedDateTime resolveDailyRun(FinancialStatementEmailSchedule schedule, ZonedDateTime base) {
        ZonedDateTime candidate = base
                .withHour(schedule.getHourOfDay())
                .withMinute(schedule.getMinuteOfHour());

        if (!candidate.isAfter(base)) {
            candidate = candidate.plusDays(1);
        }

        return candidate;
    }

    private ZonedDateTime resolveWeeklyRun(FinancialStatementEmailSchedule schedule, ZonedDateTime base) {
        DayOfWeek targetDay = DayOfWeek.of(schedule.getDayOfWeek());
        ZonedDateTime candidate = base
                .with(TemporalAdjusters.nextOrSame(targetDay))
                .withHour(schedule.getHourOfDay())
                .withMinute(schedule.getMinuteOfHour());

        if (!candidate.isAfter(base)) {
            candidate = candidate.plusWeeks(1);
        }

        return candidate;
    }

    private ZonedDateTime resolveMonthlyRun(FinancialStatementEmailSchedule schedule, ZonedDateTime base) {
        ZonedDateTime candidate = base
                .withDayOfMonth(Math.min(schedule.getDayOfMonth(), base.toLocalDate().lengthOfMonth()))
                .withHour(schedule.getHourOfDay())
                .withMinute(schedule.getMinuteOfHour());

        if (!candidate.isAfter(base)) {
            ZonedDateTime nextMonthBase = base.plusMonths(1);
            candidate = nextMonthBase
                    .withDayOfMonth(Math.min(schedule.getDayOfMonth(), nextMonthBase.toLocalDate().lengthOfMonth()))
                    .withHour(schedule.getHourOfDay())
                    .withMinute(schedule.getMinuteOfHour());
        }

        return candidate;
    }

    private FinancialStatementEntity resolveFinancialStatement(UUID reportId) {
        return financialStatementPersistencePort.findFinancialStatementByReportId(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Financial statement report not found."));
    }

    private String resolveTimezone(String timezone) {
        return StringUtils.hasText(timezone) ? timezone.trim() : defaultTimezone;
    }

    private OffsetDateTime calculateClaimLeaseUntil(OffsetDateTime referenceTime) {
        return referenceTime.plusNanos(resolvePositiveDelay(claimLeaseMs) * 1_000_000L);
    }

    private OffsetDateTime calculateFailureRetryAt(OffsetDateTime referenceTime) {
        return referenceTime.plusNanos(resolvePositiveDelay(failureRetryDelayMs) * 1_000_000L);
    }

    private long resolvePositiveDelay(long configuredDelayMs) {
        return configuredDelayMs > 0 ? configuredDelayMs : MINIMUM_SCHEDULER_DELAY_MS;
    }

    private String resolveExceptionMessage(Exception exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return "Error inesperado.";
        }
        return exception.getMessage();
    }
}
