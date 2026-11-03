package com.financeinsight.transaction.dto;

import com.financeinsight.transaction.TransactionCategory;

import java.util.UUID;

public record TransactionCategoryResponse(UUID id, TransactionCategory category,
                                          boolean categoryWasAssignedAutomatically) {
}
