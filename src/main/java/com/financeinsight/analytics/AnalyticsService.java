package com.financeinsight.analytics;

import com.financeinsight.analytics.dto.CategorySpending;
import com.financeinsight.analytics.dto.CategorySpendingResponse;
import com.financeinsight.analytics.dto.CurrencySummary;
import com.financeinsight.analytics.dto.MonthlyTrend;
import com.financeinsight.analytics.dto.SummaryResponse;
import com.financeinsight.analytics.dto.TrendsResponse;
import com.financeinsight.common.exception.InvalidRequestException;
import com.financeinsight.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class AnalyticsService {

    private static final int DEFAULT_RANGE_MONTHS = 12;
    private static final int MAX_RANGE_MONTHS = 120;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDate from, LocalDate to, UUID ownerId) {
        DateRange range = resolveRange(from, to);
        List<CurrencySummary> summaries = transactionRepository.summarizeByCurrency(range.from(), range.to(), ownerId)
                .stream()
                .map(row -> {
                    BigDecimal income = safe(row.getIncome());
                    BigDecimal expenses = safe(row.getExpenses());
                    BigDecimal refunds = safe(row.getRefunds());
                    BigDecimal netExpenses = expenses.subtract(refunds);
                    BigDecimal netSavings = income.subtract(netExpenses);
                    BigDecimal savingsRate = income.signum() == 0 ? null
                            : netSavings.multiply(BigDecimal.valueOf(100))
                                    .divide(income, 2, RoundingMode.HALF_UP);
                    return new CurrencySummary(row.getCurrencyCode(), income, expenses, refunds,
                            netExpenses, safe(row.getTransfers()), netSavings, savingsRate,
                            row.getTransactionCount() == null ? 0 : row.getTransactionCount());
                })
                .toList();
        return new SummaryResponse(range.from(), range.to(), summaries);
    }

    @Transactional(readOnly = true)
    public TrendsResponse trends(LocalDate from, LocalDate to, UUID ownerId) {
        DateRange range = resolveRange(from, to);
        List<MonthlyTrendProjection> rows = transactionRepository.monthlyTrends(range.from(), range.to(), ownerId);
        Map<String, MonthlyTrendProjection> indexed = new HashMap<>();
        Set<String> currencies = new TreeSet<>();
        for (MonthlyTrendProjection row : rows) {
            indexed.put(key(row.getCurrencyCode(), row.getMonth()), row);
            currencies.add(row.getCurrencyCode());
        }

        List<MonthlyTrend> trends = new ArrayList<>();
        for (String currency : currencies) {
            for (YearMonth month = YearMonth.from(range.from());
                 !month.isAfter(YearMonth.from(range.to())); month = month.plusMonths(1)) {
                String monthKey = month.toString();
                MonthlyTrendProjection row = indexed.get(key(currency, monthKey));
                BigDecimal income = row == null ? ZERO : safe(row.getIncome());
                BigDecimal expenses = row == null ? ZERO : safe(row.getExpenses());
                BigDecimal refunds = row == null ? ZERO : safe(row.getRefunds());
                BigDecimal netExpenses = expenses.subtract(refunds);
                trends.add(new MonthlyTrend(monthKey, currency, income, expenses, refunds,
                        netExpenses, income.subtract(netExpenses)));
            }
        }
        return new TrendsResponse(range.from(), range.to(), trends);
    }

    @Transactional(readOnly = true)
    public CategorySpendingResponse categories(LocalDate from, LocalDate to, UUID ownerId) {
        DateRange range = resolveRange(from, to);
        List<CategorySpending> categories = transactionRepository.spendingByCategory(range.from(), range.to(), ownerId)
                .stream()
                .map(row -> new CategorySpending(row.getCategory(), row.getCurrencyCode(), safe(row.getAmount()),
                        row.getTransactionCount() == null ? 0 : row.getTransactionCount()))
                .toList();
        return new CategorySpendingResponse(range.from(), range.to(), categories);
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedTo = to == null ? today : to;
        LocalDate resolvedFrom = from == null
                ? YearMonth.from(resolvedTo).minusMonths(DEFAULT_RANGE_MONTHS - 1).atDay(1)
                : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new InvalidRequestException("The start date must be on or before the end date");
        }
        long months = java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(resolvedFrom), YearMonth.from(resolvedTo)) + 1;
        if (months > MAX_RANGE_MONTHS) {
            throw new InvalidRequestException("The date range cannot exceed " + MAX_RANGE_MONTHS + " months");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String key(String currency, String month) {
        return currency + ":" + month;
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
