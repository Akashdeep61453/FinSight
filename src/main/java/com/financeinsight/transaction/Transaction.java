package com.financeinsight.transaction;

import com.financeinsight.statement.Statement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_statement_date", columnList = "statement_id, transaction_date"),
        @Index(name = "idx_transactions_category_date", columnList = "category, transaction_date")
})
public class Transaction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, length = 500)
    private String description;

    /** Absolute transaction value; direction determines whether it is income or spending. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionCategory category;

    @Column(name = "category_explanation", length = 255)
    private String categoryExplanation;

    @Column(name = "category_was_assigned_automatically", nullable = false)
    private boolean categoryWasAssignedAutomatically = true;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "source_row")
    private Integer sourceRow;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transaction() {
    }

    public Transaction(LocalDate transactionDate, String description, BigDecimal amount,
                       TransactionDirection direction, TransactionCategory category) {
        this.transactionDate = transactionDate;
        this.description = description;
        this.amount = amount;
        this.direction = direction;
        this.category = category;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (category == null) {
            category = TransactionCategory.UNCATEGORIZED;
        }
}

    public UUID getId() { return id; }
    public Statement getStatement() { return statement; }
    public void setStatement(Statement statement) { this.statement = statement; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionDirection getDirection() { return direction; }
    public void setDirection(TransactionDirection direction) { this.direction = direction; }
    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
    public String getCategoryExplanation() { return categoryExplanation; }
    public void setCategoryExplanation(String categoryExplanation) { this.categoryExplanation = categoryExplanation; }
    public boolean isCategoryWasAssignedAutomatically() { return categoryWasAssignedAutomatically; }
    public void setCategoryWasAssignedAutomatically(boolean value) { this.categoryWasAssignedAutomatically = value; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public Integer getSourcePage() { return sourcePage; }
    public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
    public Integer getSourceRow() { return sourceRow; }
    public void setSourceRow(Integer sourceRow) { this.sourceRow = sourceRow; }
    public Instant getCreatedAt() { return createdAt; }
}



