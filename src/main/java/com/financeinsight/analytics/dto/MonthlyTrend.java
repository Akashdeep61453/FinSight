package com.financeinsight.analytics.dto;

import java.math.BigDecimal;

public record MonthlyTrend(String month, String currencyCode, BigDecimal income, BigDecimal expenses,
                           BigDecimal refunds, BigDecimal netExpenses, BigDecimal netSavings) {
}
