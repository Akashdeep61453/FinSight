package com.financeinsight.transaction.dto;

import com.financeinsight.transaction.TransactionCategory;
import com.financeinsight.transaction.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID statementId,
        String currencyCode,
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionDirection direction,
        TransactionCategory category,
        boolean categoryWasAssignedAutomatically,
        String categoryExplanation,
        BigDecimal balanceAfter,
        Integer sourcePage,
        Integer sourceRow
) {
}
