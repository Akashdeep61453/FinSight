package com.financeinsight.parser.dto;

import java.util.List;

public record ParsedStatement(String parserName, String currencyCode,
                              List<ParsedTransaction> transactions, List<String> warnings) {
    public ParsedStatement {
        transactions = List.copyOf(transactions);
        warnings = List.copyOf(warnings);
    }
}
