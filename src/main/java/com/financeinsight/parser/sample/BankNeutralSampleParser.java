package com.financeinsight.parser.sample;

import com.financeinsight.parser.ExtractedPdfText;
import com.financeinsight.parser.StatementParser;
import com.financeinsight.parser.dto.ParsedStatement;
import com.financeinsight.parser.dto.ParsedTransaction;
import com.financeinsight.transaction.TransactionDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for the repository's synthetic fixture format. This is not a real bank parser. */
@Component
public class BankNeutralSampleParser implements StatementParser {

    private static final Pattern CURRENCY = Pattern.compile("(?im)^\\s*Currency:\\s*([A-Z]{3})\\s*$");

    private static final Pattern ROW = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})\\s+(.+?)\\s{2,}(-|[\\d,]+(?:\\.\\d{1,4})?)"
                    + "\\s{2,}(-|[\\d,]+(?:\\.\\d{1,4})?)"
                    + "\\s{2,}(-|[\\d,]+(?:\\.\\d{1,4})?)\\s*$");

    @Override
    public String name() {
        return "synthetic-sample-v1";
    }

    @Override
    public boolean supports(List<String> pages) {
        String allText = String.join("\n", pages).toLowerCase(Locale.ROOT);
        return allText.contains("bank statement sample")
                && allText.contains("date")
                && allText.contains("description")
                && allText.contains("debit")
                && allText.contains("credit")
                && allText.contains("balance");
    }

    @Override
    public ParsedStatement parse(ExtractedPdfText document) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String currencyCode = extractCurrency(document);
        if (currencyCode == null) {
            warnings.add("Currency code is missing or invalid.");
        }

        for (int pageIndex = 0; pageIndex < document.pages().size(); pageIndex++) {
            String[] lines = document.pages().get(pageIndex).split("\\R");
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex].strip();
                if (line.isEmpty() || isHeaderOrMetadata(line)) {
                    continue;
                }

                Matcher matcher = ROW.matcher(line);
                if (!matcher.matches()) {
                    if (looksLikeTransaction(line)) {
                        warnings.add(rowWarning(pageIndex + 1, lineIndex + 1,
                                "Transaction row did not match the expected columns"));
                    }
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(matcher.group(1));
                    String description = matcher.group(2).trim();
                    BigDecimal debit = parseAmount(matcher.group(3));
                    BigDecimal credit = parseAmount(matcher.group(4));
                    BigDecimal balance = parseAmount(matcher.group(5));

                    if (description.isBlank()) {
                        warnings.add(rowWarning(pageIndex + 1, lineIndex + 1, "Description is empty"));
                        continue;
                    }
                    if (debit != null && credit != null) {
                        warnings.add(rowWarning(pageIndex + 1, lineIndex + 1,
                                "Both debit and credit values are populated"));
                        continue;
                    }
                    if (debit == null && credit == null) {
                        warnings.add(rowWarning(pageIndex + 1, lineIndex + 1,
                                "Neither debit nor credit value is populated"));
                        continue;
                    }

                    BigDecimal amount = debit != null ? debit : credit;
                    if (amount.signum() <= 0) {
                        warnings.add(rowWarning(pageIndex + 1, lineIndex + 1,
                                "Transaction amount must be greater than zero"));
                        continue;
                    }
                    TransactionDirection direction = debit != null
                            ? TransactionDirection.DEBIT : TransactionDirection.CREDIT;
                    transactions.add(new ParsedTransaction(date, description, amount, direction, balance,
                            pageIndex + 1, lineIndex + 1));
                } catch (DateTimeParseException | NumberFormatException exception) {
                    warnings.add(rowWarning(pageIndex + 1, lineIndex + 1,
                            "Date or amount could not be parsed"));
                }
            }
        }

        if (transactions.isEmpty()) {
            warnings.add("No transactions were parsed from the recognized sample layout.");
        }
        return new ParsedStatement(name(), currencyCode, transactions, warnings);
    }

    private String extractCurrency(ExtractedPdfText document) {
        Matcher matcher = CURRENCY.matcher(String.join("\n", document.pages()));
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private boolean isHeaderOrMetadata(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("bank statement sample")
                || lower.startsWith("account:")
                || lower.startsWith("currency:")
                || lower.startsWith("period:")
                || lower.startsWith("date ") && lower.contains("description")
                || lower.startsWith("date\t") && lower.contains("description");
    }

    private boolean looksLikeTransaction(String line) {
        return line.matches("^\\d{4}-\\d{2}-\\d{2}\\b.*");
    }

    private BigDecimal parseAmount(String value) {
        if (value.equals("-")) {
            return null;
        }
        return new BigDecimal(value.replace(",", ""));
    }

    private String rowWarning(int page, int row, String reason) {
        return "Page " + page + ", row " + row + ": " + reason + ".";
    }
}




