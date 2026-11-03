package com.financeinsight.common.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction was not found");
    }
}
