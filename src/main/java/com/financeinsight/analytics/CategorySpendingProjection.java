package com.financeinsight.analytics;

import java.math.BigDecimal;

public interface CategorySpendingProjection {
    String getCurrencyCode();
    String getCategory();
    BigDecimal getAmount();
    Long getTransactionCount();
}
