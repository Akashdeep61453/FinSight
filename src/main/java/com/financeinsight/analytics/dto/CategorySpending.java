package com.financeinsight.analytics.dto;

import java.math.BigDecimal;

public record CategorySpending(String category, String currencyCode, BigDecimal amount,
                               long transactionCount) {
}
