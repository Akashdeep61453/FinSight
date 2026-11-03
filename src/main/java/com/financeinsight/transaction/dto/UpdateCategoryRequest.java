package com.financeinsight.transaction.dto;

import com.financeinsight.transaction.TransactionCategory;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequest(@NotNull TransactionCategory category) {
}
