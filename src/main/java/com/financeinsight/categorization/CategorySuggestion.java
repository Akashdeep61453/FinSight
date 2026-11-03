package com.financeinsight.categorization;

import com.financeinsight.transaction.TransactionCategory;

public record CategorySuggestion(TransactionCategory category, String explanation) {
}
