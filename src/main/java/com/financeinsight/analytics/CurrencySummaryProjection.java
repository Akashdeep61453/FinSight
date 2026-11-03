package com.financeinsight.analytics;

import java.math.BigDecimal;

public interface CurrencySummaryProjection {
    String getCurrencyCode();
    BigDecimal getIncome();
    BigDecimal getExpenses();
    BigDecimal getRefunds();
    BigDecimal getTransfers();
    Long getTransactionCount();
}
