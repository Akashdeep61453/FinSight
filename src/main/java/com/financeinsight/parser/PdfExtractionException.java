package com.financeinsight.parser;

public class PdfExtractionException extends RuntimeException {

    private final PdfExtractionFailure failure;

    public PdfExtractionException(PdfExtractionFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public PdfExtractionException(PdfExtractionFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public PdfExtractionFailure getFailure() {
        return failure;
    }
}
