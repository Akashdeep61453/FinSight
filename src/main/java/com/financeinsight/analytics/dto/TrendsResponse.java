package com.financeinsight.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record TrendsResponse(LocalDate from, LocalDate to, List<MonthlyTrend> trends) {
    public TrendsResponse {
        trends = List.copyOf(trends);
    }
}
