package com.financeinsight.parser.sample;

import com.financeinsight.parser.ExtractedPdfText;
import com.financeinsight.parser.ParserRouter;
import com.financeinsight.parser.UnsupportedStatementFormatException;
import com.financeinsight.parser.dto.ParsedStatement;
import com.financeinsight.parser.dto.ParsedTransaction;
import com.financeinsight.transaction.TransactionDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankNeutralSampleParserTest {

    private final BankNeutralSampleParser parser = new BankNeutralSampleParser();

    @Test
    void parsesTheSyntheticFixtureRowsAndNormalizesCurrency() {
        ExtractedPdfText document = document("""
                BANK STATEMENT SAMPLE
                Account: DEMO-0001
                Currency: inr
                Period: 2026-01-01 to 2026-01-31

                Date        Description                 Debit       Credit      Balance
                2026-01-02  SALARY SAMPLE EMPLOYER       -           75000.00    75000.00
                2026-01-03  SAMPLE GROCERY STORE         2450.50     -           72549.50
                2026-01-05  SAMPLE ELECTRIC BILL         1800.00     -           70749.50
                2026-01-10  SAMPLE RENT PAYMENT          22000.00    -           48749.50
                2026-01-12  SAMPLE PHARMACY               620.00     -           48129.50
                2026-01-20  SAMPLE TRANSFER REFUND        -           500.00      48629.50
                """);

        ParsedStatement parsed = parser.parse(document);

        assertEquals("INR", parsed.currencyCode());
        assertEquals(6, parsed.transactions().size());
        assertTrue(parsed.warnings().isEmpty());
        ParsedTransaction salary = parsed.transactions().getFirst();
        assertEquals(LocalDate.parse("2026-01-02"), salary.transactionDate());
        assertEquals("SALARY SAMPLE EMPLOYER", salary.description());
        assertEquals(new BigDecimal("75000.00"), salary.amount());
        assertEquals(TransactionDirection.CREDIT, salary.direction());
        assertEquals(new BigDecimal("75000.00"), salary.balanceAfter());
        assertEquals(1, salary.sourcePage());
    }

    @Test
    void reportsMalformedAndAmbiguousRowsButKeepsValidRows() {
        ExtractedPdfText document = document("""
                BANK STATEMENT SAMPLE
                Currency: INR
                Date        Description                 Debit       Credit      Balance
                2026-01-02  SAMPLE GROCERIES             20.00       -           80.00
                2026-01-03  SAMPLE DOUBLE ENTRY          10.00       10.00       80.00
                2026-13-40  SAMPLE BAD DATE              5.00        -           75.00
                2026-01-05  SAMPLE BROKEN AMOUNT          nope        -           70.00
                """);

        ParsedStatement parsed = parser.parse(document);

        assertEquals(1, parsed.transactions().size());
        assertEquals(3, parsed.warnings().size());
        assertTrue(parsed.warnings().stream().anyMatch(message -> message.contains("Both debit and credit")));
        assertTrue(parsed.warnings().stream().anyMatch(message -> message.contains("Date or amount")));
        assertTrue(parsed.warnings().stream().anyMatch(message -> message.contains("expected columns")));
        assertFalse(parsed.warnings().stream().anyMatch(message -> message.contains("SAMPLE DOUBLE ENTRY")),
                "Warnings should not disclose statement descriptions");
    }

    @Test
    void preservesPageAndRowProvenanceAcrossPageBreaks() {
        ExtractedPdfText document = new ExtractedPdfText(List.of(
                "BANK STATEMENT SAMPLE\nCurrency: INR\nDate Description Debit Credit Balance\n"
                        + "2026-01-02  FIRST PAGE ITEM  5.00  -  95.00",
                "Date Description Debit Credit Balance\n"
                        + "2026-01-03  SECOND PAGE ITEM  7.00  -  88.00"));

        ParsedStatement parsed = parser.parse(document);

        assertEquals(2, parsed.transactions().size());
        assertEquals(2, parsed.transactions().get(1).sourcePage());
        assertEquals(2, parsed.transactions().get(1).sourceRow());
    }

    @Test
    void marksMissingCurrencyForReviewAndRejectsUnknownLayouts() {
        ParsedStatement parsed = parser.parse(document("""
                BANK STATEMENT SAMPLE
                Date Description Debit Credit Balance
                2026-01-02  SAMPLE ITEM  1.00  -  9.00
                """));
        assertNull(parsed.currencyCode());
        assertTrue(parsed.warnings().stream().anyMatch(message -> message.contains("Currency code")));

        ParserRouter router = new ParserRouter(List.of(parser));
        assertThrows(UnsupportedStatementFormatException.class,
                () -> router.parse(new ExtractedPdfText(List.of("An unknown statement layout"))));
    }

    private ExtractedPdfText document(String page) {
        return new ExtractedPdfText(List.of(page.stripIndent().strip()));
    }
}
