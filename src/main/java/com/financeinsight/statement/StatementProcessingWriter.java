package com.financeinsight.statement;

import com.financeinsight.categorization.Categorizer;
import com.financeinsight.categorization.CategorySuggestion;
import com.financeinsight.common.exception.StatementNotFoundException;
import com.financeinsight.parser.dto.ParsedStatement;
import com.financeinsight.parser.dto.ParsedTransaction;
import com.financeinsight.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class StatementProcessingWriter {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final StatementRepository statementRepository;
    private final Categorizer categorizer;

    public StatementProcessingWriter(StatementRepository statementRepository, Categorizer categorizer) {
        this.statementRepository = statementRepository;
        this.categorizer = categorizer;
    }

    @Transactional
    public void start(UUID statementId) {
        Statement statement = findStatement(statementId);
        statement.setStatus(StatementStatus.PROCESSING);
        statement.setProcessingMessage(null);
        statement.setParserName(null);
    }

    @Transactional
    public void finish(UUID statementId, ParsedStatement parsedStatement) {
        Statement statement = findStatement(statementId);
        for (Transaction oldTransaction : new ArrayList<>(statement.getTransactions())) {
            statement.removeTransaction(oldTransaction);
        }

        for (ParsedTransaction parsed : parsedStatement.transactions()) {
            CategorySuggestion suggestion = categorizer.categorize(parsed);
            Transaction transaction = new Transaction(parsed.transactionDate(), parsed.description(), parsed.amount(),
                    parsed.direction(), suggestion.category());
            transaction.setCategoryWasAssignedAutomatically(true);
            transaction.setCategoryExplanation(suggestion.explanation());
            transaction.setBalanceAfter(parsed.balanceAfter());
            transaction.setSourcePage(parsed.sourcePage());
            transaction.setSourceRow(parsed.sourceRow());
            statement.addTransaction(transaction);
        }

        statement.setParserName(parsedStatement.parserName());
        statement.setCurrencyCode(parsedStatement.currencyCode());
        statement.setProcessingMessage(joinAndLimit(parsedStatement.warnings()));
        boolean needsReview = parsedStatement.transactions().isEmpty() || !parsedStatement.warnings().isEmpty();
        statement.setStatus(needsReview ? StatementStatus.NEEDS_REVIEW : StatementStatus.COMPLETED);
    }

    @Transactional
    public void needsReview(UUID statementId, String message) {
        Statement statement = findStatement(statementId);
        statement.setStatus(StatementStatus.NEEDS_REVIEW);
        statement.setProcessingMessage(limit(message));
    }

    @Transactional
    public void failed(UUID statementId, String message) {
        Statement statement = findStatement(statementId);
        statement.setStatus(StatementStatus.FAILED);
        statement.setProcessingMessage(limit(message));
    }

    private Statement findStatement(UUID id) {
        return statementRepository.findById(id).orElseThrow(StatementNotFoundException::new);
    }

    private String joinAndLimit(java.util.List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }
        return limit(String.join(" ", warnings));
    }

    private String limit(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }
}

