package com.financeinsight.analytics.dto;

import java.math.BigDecimal;

public record CurrencySummary(String currencyCode, BigDecimal income, BigDecimal expenses,
                              BigDecimal refunds, BigDecimal netExpenses, BigDecimal transfers,
                              BigDecimal netSavings, BigDecimal savingsRatePercent, long transactionCount) {
}
