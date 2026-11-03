package com.financeinsight.parser.dto;

import com.financeinsight.transaction.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionDirection direction,
        BigDecimal balanceAfter,
        int sourcePage,
        int sourceRow
) {
}
