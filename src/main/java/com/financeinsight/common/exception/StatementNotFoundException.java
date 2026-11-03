package com.financeinsight.common.exception;

public class StatementNotFoundException extends RuntimeException {
    public StatementNotFoundException() {
        super("Statement was not found");
    }
}
