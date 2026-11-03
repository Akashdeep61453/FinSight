package com.financeinsight.parser;

public class UnsupportedStatementFormatException extends RuntimeException {
    public UnsupportedStatementFormatException() {
        super("This statement format is not recognized. The statement needs review.");
    }
}
