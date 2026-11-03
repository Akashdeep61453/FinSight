package com.financeinsight.categorization;

import com.financeinsight.parser.dto.ParsedTransaction;
import com.financeinsight.transaction.TransactionCategory;
import com.financeinsight.transaction.TransactionDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedCategorizerTest {

    private final RuleBasedCategorizer categorizer = new RuleBasedCategorizer();

    @Test
    void recognizesIncomeRefundsAndCommonSpending() {
        assertEquals(TransactionCategory.INCOME, suggest("MONTHLY SALARY", TransactionDirection.CREDIT).category());
        assertEquals(TransactionCategory.REFUND, suggest("CARD PURCHASE REFUND", TransactionDirection.CREDIT).category());
        assertEquals(TransactionCategory.GROCERIES, suggest("SAMPLE GROCERY STORE", TransactionDirection.DEBIT).category());
        assertEquals(TransactionCategory.HEALTHCARE, suggest("SAMPLE PHARMACY", TransactionDirection.DEBIT).category());
    }

    @Test
    void leavesUnknownCreditsUncategorizedInsteadOfTreatingThemAsIncome() {
        CategorySuggestion suggestion = suggest("UNRECOGNIZED BANK CREDIT", TransactionDirection.CREDIT);
        assertEquals(TransactionCategory.UNCATEGORIZED, suggestion.category());
        assertTrue(suggestion.explanation().contains("review"));
    }

    @Test
    void categorizesTransfersBeforeMerchantRules() {
        assertEquals(TransactionCategory.TRANSFER,
                suggest("UPI TRANSFER TO GROCERY STORE", TransactionDirection.DEBIT).category());
    }

    private CategorySuggestion suggest(String description, TransactionDirection direction) {
        return categorizer.categorize(new ParsedTransaction(LocalDate.parse("2026-01-01"), description,
                new BigDecimal("10.00"), direction, null, 1, 1));
    }
}

