package com.unicauca.edu.co.financial_statements.infrastructure.out.persistence.entity;

import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportDeliveryFrequency;
import com.unicauca.edu.co.financial_statements.domain.models.enums.EReportExportFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "FINANCIAL_STATEMENT_EMAIL_SCHEDULE",
        indexes = {
                @Index(name = "idx_financial_statement_email_schedule_next_run", columnList = "active,nextRunAt"),
                @Index(name = "idx_financial_statement_email_schedule_statement_id", columnList = "financial_statement_id")
        }
)
public class FinancialStatementEmailScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_statement_id", nullable = false)
    private FinancialStatementEntity financialStatement;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EReportExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EReportDeliveryFrequency frequency;

    @Column(nullable = false)
    private Integer hourOfDay;

    @Column(nullable = false)
    private Integer minuteOfHour;

    @Column
    private Integer dayOfWeek;

    @Column
    private Integer dayOfMonth;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private OffsetDateTime nextRunAt;

    @Column
    private OffsetDateTime lastRunAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
