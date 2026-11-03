package com.financeinsight.statement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, UUID> {
    Optional<Statement> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<Statement> findAllByUploadedAtBeforeAndStatusIn(java.time.Instant cutoff,
                                                         java.util.Collection<StatementStatus> statuses);
    void deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
