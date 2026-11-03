package com.financeinsight.statement;

import com.financeinsight.storage.StatementStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StatementRetentionService {
    private static final Logger log = LoggerFactory.getLogger(StatementRetentionService.class);
    private static final List<StatementStatus> CLEANABLE_STATUSES = List.of(
            StatementStatus.UPLOADED, StatementStatus.COMPLETED,
            StatementStatus.NEEDS_REVIEW, StatementStatus.FAILED);

    private final StatementRepository statements;
    private final StatementStorage storage;
    private final long retentionDays;
    private final boolean enabled;

    public StatementRetentionService(StatementRepository statements, StatementStorage storage,
                                    @Value("${app.retention.days:365}") long retentionDays,
                                    @Value("${app.retention.enabled:true}") boolean enabled) {
        if (retentionDays < 1) throw new IllegalArgumentException("PDF retention must be at least one day");
        this.statements = statements;
        this.storage = storage;
        this.retentionDays = retentionDays;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${app.retention.cleanup-cron:0 30 2 * * *}")
    public void cleanupExpiredStatements() {
        if (!enabled) return;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<Statement> expired = statements.findAllByUploadedAtBeforeAndStatusIn(cutoff, CLEANABLE_STATUSES);
        int removed = 0;
        for (Statement statement : expired) {
            try {
                if (statement.getStorageKey() != null) storage.delete(statement.getStorageKey());
                statements.delete(statement);
                removed++;
            } catch (IOException | RuntimeException exception) {
                log.warn("Could not remove expired statement {} ({})", statement.getId(),
                        exception.getClass().getSimpleName());
            }
        }
        if (removed > 0) log.info("Removed {} expired statement(s)", removed);
    }
}
