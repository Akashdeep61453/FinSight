package com.financeinsight.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record CategorySpendingResponse(LocalDate from, LocalDate to, List<CategorySpending> categories) {
    public CategorySpendingResponse {
        categories = List.copyOf(categories);
    }
}
