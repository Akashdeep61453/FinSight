package com.financeinsight.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record SummaryResponse(LocalDate from, LocalDate to, List<CurrencySummary> currencies) {
    public SummaryResponse {
        currencies = List.copyOf(currencies);
    }
}
