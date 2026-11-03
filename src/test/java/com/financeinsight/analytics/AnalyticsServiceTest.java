package com.financeinsight.analytics;

import com.financeinsight.analytics.dto.CurrencySummary;
import com.financeinsight.analytics.dto.MonthlyTrend;
import com.financeinsight.common.exception.InvalidRequestException;
import com.financeinsight.transaction.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final AnalyticsService service = new AnalyticsService(repository);
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void calculatesNetSavingsAndSavingsRateByCurrency() {
        CurrencySummaryProjection row = mock(CurrencySummaryProjection.class);
        when(row.getCurrencyCode()).thenReturn("INR");
        when(row.getIncome()).thenReturn(new BigDecimal("1000.00"));
        when(row.getExpenses()).thenReturn(new BigDecimal("500.00"));
        when(row.getRefunds()).thenReturn(new BigDecimal("50.00"));
        when(row.getTransfers()).thenReturn(new BigDecimal("200.00"));
        when(row.getTransactionCount()).thenReturn(8L);
        when(repository.summarizeByCurrency(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), ownerId))
                .thenReturn(List.of(row));

        CurrencySummary result = service.summary(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), ownerId)
                .currencies().getFirst();

        assertEquals("INR", result.currencyCode());
        assertEquals(new BigDecimal("500.00"), result.expenses());
        assertEquals(new BigDecimal("50.00"), result.refunds());
        assertEquals(new BigDecimal("450.00"), result.netExpenses());
        assertEquals(new BigDecimal("550.00"), result.netSavings());
        assertEquals(new BigDecimal("55.00"), result.savingsRatePercent());
        assertEquals(new BigDecimal("200.00"), result.transfers());
    }

    @Test
    void fillsMissingMonthsWithZeroValuesForEachCurrency() {
        MonthlyTrendProjection january = trend("INR", "2026-01", "900.00", "400.00", "0.00");
        MonthlyTrendProjection march = trend("INR", "2026-03", "1000.00", "600.00", "100.00");
        when(repository.monthlyTrends(any(), any(), eq(ownerId))).thenReturn(List.of(january, march));

        List<MonthlyTrend> trends = service.trends(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-03-31"), ownerId).trends();

        assertEquals(3, trends.size());
        assertEquals("2026-02", trends.get(1).month());
        assertEquals(BigDecimal.ZERO, trends.get(1).income());
        assertEquals(BigDecimal.ZERO, trends.get(1).netExpenses());
        assertEquals(new BigDecimal("500.00"), trends.get(2).netExpenses());
        assertEquals(new BigDecimal("500.00"), trends.get(2).netSavings());
    }

    @Test
    void rejectsReversedOrOverlyWideDateRanges() {
        assertThrows(InvalidRequestException.class,
                () -> service.summary(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-01"), ownerId));
        assertThrows(InvalidRequestException.class,
                () -> service.trends(LocalDate.parse("2000-01-01"), LocalDate.parse("2010-01-01"), ownerId));
        verifyNoInteractions(repository);
    }

    private MonthlyTrendProjection trend(String currency, String month,
                                         String income, String expenses, String refunds) {
        MonthlyTrendProjection row = mock(MonthlyTrendProjection.class);
        when(row.getCurrencyCode()).thenReturn(currency);
        when(row.getMonth()).thenReturn(month);
        when(row.getIncome()).thenReturn(new BigDecimal(income));
        when(row.getExpenses()).thenReturn(new BigDecimal(expenses));
        when(row.getRefunds()).thenReturn(new BigDecimal(refunds));
        return row;
    }
}
