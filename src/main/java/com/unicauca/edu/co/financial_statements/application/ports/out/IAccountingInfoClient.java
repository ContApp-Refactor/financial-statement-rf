package com.unicauca.edu.co.financial_statements.application.ports.out;

import com.unicauca.edu.co.financial_statements.domain.models.external.accountingInfo.AccountingEntry;

import java.time.LocalDate;
import java.util.List;

public interface IAccountingInfoClient {

    List<AccountingEntry> findAccountingEntries(String entId, LocalDate startDate, LocalDate endDate);
}
