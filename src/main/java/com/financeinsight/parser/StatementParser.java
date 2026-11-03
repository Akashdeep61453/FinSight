package com.financeinsight.parser;

import com.financeinsight.parser.dto.ParsedStatement;

import java.util.List;

public interface StatementParser {
    String name();
    boolean supports(List<String> pages);
    ParsedStatement parse(ExtractedPdfText document);
}
