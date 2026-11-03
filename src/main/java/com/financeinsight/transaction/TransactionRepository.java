package com.financeinsight.transaction;

import com.financeinsight.analytics.CategorySpendingProjection;
import com.financeinsight.analytics.CurrencySummaryProjection;
import com.financeinsight.analytics.MonthlyTrendProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    java.util.Optional<Transaction> findByIdAndStatementOwnerId(UUID id, UUID ownerId);

    @Query(value = """
            SELECT s.currency_code AS "currencyCode",
                   COALESCE(SUM(CASE WHEN t.direction = 'CREDIT' AND t.category = 'INCOME'
                                     THEN t.amount ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN t.direction = 'DEBIT' AND t.category <> 'TRANSFER'
                                     THEN t.amount ELSE 0 END), 0) AS expenses,
                   COALESCE(SUM(CASE WHEN t.direction = 'CREDIT' AND t.category = 'REFUND'
                                     THEN t.amount ELSE 0 END), 0) AS refunds,
                   COALESCE(SUM(CASE WHEN t.category = 'TRANSFER' THEN t.amount ELSE 0 END), 0) AS transfers,
                   COUNT(*) AS "transactionCount"
            FROM transactions t
            JOIN statements s ON s.id = t.statement_id
            WHERE t.transaction_date BETWEEN :fromDate AND :toDate
              AND s.owner_id = :ownerId
              AND s.currency_code IS NOT NULL
            GROUP BY s.currency_code
            ORDER BY s.currency_code
            """, nativeQuery = true)
    List<CurrencySummaryProjection> summarizeByCurrency(@Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate,
                                                         @Param("ownerId") UUID ownerId);

    @Query(value = """
            SELECT s.currency_code AS "currencyCode",
                   TO_CHAR(t.transaction_date, 'YYYY-MM') AS month,
                   COALESCE(SUM(CASE WHEN t.direction = 'CREDIT' AND t.category = 'INCOME'
                                     THEN t.amount ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN t.direction = 'DEBIT' AND t.category <> 'TRANSFER'
                                     THEN t.amount ELSE 0 END), 0) AS expenses,
                   COALESCE(SUM(CASE WHEN t.direction = 'CREDIT' AND t.category = 'REFUND'
                                     THEN t.amount ELSE 0 END), 0) AS refunds
            FROM transactions t
            JOIN statements s ON s.id = t.statement_id
            WHERE t.transaction_date BETWEEN :fromDate AND :toDate
              AND s.owner_id = :ownerId
              AND s.currency_code IS NOT NULL
            GROUP BY s.currency_code, TO_CHAR(t.transaction_date, 'YYYY-MM')
            ORDER BY s.currency_code, TO_CHAR(t.transaction_date, 'YYYY-MM')
            """, nativeQuery = true)
    List<MonthlyTrendProjection> monthlyTrends(@Param("fromDate") LocalDate fromDate,
                                               @Param("toDate") LocalDate toDate,
                                               @Param("ownerId") UUID ownerId);

    @Query(value = """
            SELECT s.currency_code AS "currencyCode",
                   t.category AS category,
                   COALESCE(SUM(t.amount), 0) AS amount,
                   COUNT(*) AS "transactionCount"
            FROM transactions t
            JOIN statements s ON s.id = t.statement_id
            WHERE t.transaction_date BETWEEN :fromDate AND :toDate
              AND s.owner_id = :ownerId
              AND t.direction = 'DEBIT'
              AND t.category <> 'TRANSFER'
              AND s.currency_code IS NOT NULL
            GROUP BY s.currency_code, t.category
            ORDER BY s.currency_code, amount DESC
            """, nativeQuery = true)
    List<CategorySpendingProjection> spendingByCategory(@Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate,
                                                         @Param("ownerId") UUID ownerId);
}

