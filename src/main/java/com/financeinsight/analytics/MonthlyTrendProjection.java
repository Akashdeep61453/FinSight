package com.financeinsight.analytics;

import java.math.BigDecimal;

public interface MonthlyTrendProjection {
    String getCurrencyCode();
    String getMonth();
    BigDecimal getIncome();
    BigDecimal getExpenses();
    BigDecimal getRefunds();
}
