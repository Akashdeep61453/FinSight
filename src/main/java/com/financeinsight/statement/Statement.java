package com.financeinsight.statement;

import com.financeinsight.transaction.Transaction;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "statements", indexes = {
        @Index(name = "idx_statements_owner_uploaded", columnList = "owner_id, uploaded_at")
})
public class Statement {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** Null until user accounts are introduced. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "parser_name", length = 100)
    private String parserName;

    @Column(name = "processing_message", length = 2000)
    private String processingMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatementStatus status;

    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    protected Statement() {
    }

    public Statement(String originalFilename, String bankName, String storageKey) {
        this.originalFilename = originalFilename;
        this.bankName = bankName;
        this.storageKey = storageKey;
        this.status = StatementStatus.UPLOADED;
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (status == null) {
            status = StatementStatus.UPLOADED;
        }
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setStatement(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setStatement(null);
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getBankName() { return bankName; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getStorageKey() { return storageKey; }
    public String getParserName() { return parserName; }
    public void setParserName(String parserName) { this.parserName = parserName; }
    public String getProcessingMessage() { return processingMessage; }
    public void setProcessingMessage(String processingMessage) { this.processingMessage = processingMessage; }
    public Instant getUploadedAt() { return uploadedAt; }
    public StatementStatus getStatus() { return status; }
    public void setStatus(StatementStatus status) { this.status = status; }
    public List<Transaction> getTransactions() { return transactions; }
}





