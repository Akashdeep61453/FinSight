package com.financeinsight.statement;

import com.financeinsight.common.exception.InvalidUploadException;
import com.financeinsight.common.exception.StatementNotFoundException;
import com.financeinsight.statement.dto.StatementStatusResponse;
import com.financeinsight.statement.dto.StatementUploadResponse;
import com.financeinsight.storage.StatementStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class StatementService {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

    private final StatementRepository statementRepository;
    private final StatementStorage statementStorage;
    private final StatementProcessingService processingService;
    private final long maxUploadBytes;

    public StatementService(StatementRepository statementRepository,
                            StatementStorage statementStorage,
                            StatementProcessingService processingService,
                            @Value("${app.upload.max-bytes:10485760}") long maxUploadBytes) {
        this.statementRepository = statementRepository;
        this.statementStorage = statementStorage;
        this.processingService = processingService;
        this.maxUploadBytes = Math.min(maxUploadBytes, MAX_UPLOAD_BYTES);
    }

    public StatementUploadResponse upload(MultipartFile file, UUID ownerId) {
        validate(file);
        String filename = safeOriginalFilename(file.getOriginalFilename());
        String storageKey;
        try (InputStream content = file.getInputStream()) {
            storageKey = statementStorage.store(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store uploaded statement", exception);
        }

        Statement saved;
        try {
            Statement statement = new Statement(filename, null, storageKey);
            statement.setOwnerId(ownerId);
            saved = statementRepository.save(statement);
        } catch (RuntimeException exception) {
            try {
                statementStorage.delete(storageKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }

        processingService.process(saved.getId());
        StatementStatusResponse result = getStatus(saved.getId(), ownerId);
        return new StatementUploadResponse(result.id(), result.originalFilename(), result.bankName(), result.currencyCode(),
                result.status(), result.uploadedAt(), result.parserName(), result.processingMessage());
    }

    public StatementStatusResponse getStatus(UUID id, UUID ownerId) {
        Statement statement = statementRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(StatementNotFoundException::new);
        return new StatementStatusResponse(statement.getId(), statement.getOriginalFilename(),
                statement.getBankName(), statement.getCurrencyCode(), statement.getStatus(), statement.getUploadedAt(),
                statement.getParserName(), statement.getProcessingMessage());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Select a non-empty PDF file");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new InvalidUploadException("The PDF exceeds the upload size limit");
        }
        String filename = safeOriginalFilename(file.getOriginalFilename());
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidUploadException("Only PDF files are accepted");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(5);
            if (!"%PDF-".equals(new String(signature, StandardCharsets.US_ASCII))) {
                throw new InvalidUploadException("The uploaded file does not have a valid PDF signature");
            }
        } catch (IOException exception) {
            throw new InvalidUploadException("The uploaded file could not be read");
        }
    }

    private String safeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidUploadException("A filename is required");
        }
        String leaf = originalFilename.replace('\\', '/');
        leaf = leaf.substring(leaf.lastIndexOf('/') + 1).trim();
        if (leaf.isBlank() || leaf.length() > 255 || leaf.contains("..")) {
            throw new InvalidUploadException("The filename is invalid");
        }
        return leaf;
    }
}


