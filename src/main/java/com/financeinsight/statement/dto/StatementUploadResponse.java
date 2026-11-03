package com.financeinsight.statement.dto;

import com.financeinsight.statement.StatementStatus;

import java.time.Instant;
import java.util.UUID;

public record StatementUploadResponse(UUID id, String originalFilename, String bankName,
                                      String currencyCode, StatementStatus status, Instant uploadedAt,
                                      String parserName, String processingMessage) {
}
