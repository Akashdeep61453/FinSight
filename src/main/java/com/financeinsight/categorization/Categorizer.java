package com.financeinsight.categorization;

import com.financeinsight.parser.dto.ParsedTransaction;

public interface Categorizer {
    CategorySuggestion categorize(ParsedTransaction transaction);
}
