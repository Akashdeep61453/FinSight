package com.financeinsight.parser;

import com.financeinsight.parser.dto.ParsedStatement;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserRouter {

    private final List<StatementParser> parsers;

    public ParserRouter(List<StatementParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public ParsedStatement parse(ExtractedPdfText document) {
        List<StatementParser> matches = parsers.stream()
                .filter(parser -> parser.supports(document.pages()))
                .toList();
        if (matches.size() != 1) {
            throw new UnsupportedStatementFormatException();
        }
        return matches.getFirst().parse(document);
    }
}
