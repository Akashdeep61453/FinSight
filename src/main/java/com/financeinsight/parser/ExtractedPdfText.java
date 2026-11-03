package com.financeinsight.parser;

import java.util.List;

public record ExtractedPdfText(List<String> pages) {

    public ExtractedPdfText {
        pages = List.copyOf(pages);
    }

    public int pageCount() {
        return pages.size();
    }

    public String fullText() {
        return String.join("\n\n", pages);
    }
}
