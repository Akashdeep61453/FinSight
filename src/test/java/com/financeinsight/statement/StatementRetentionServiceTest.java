package com.financeinsight.statement;

import com.financeinsight.storage.StatementStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

class StatementRetentionServiceTest {
    private final StatementRepository repository = mock(StatementRepository.class);
    private final StatementStorage storage = mock(StatementStorage.class);

    @Test
    void removesStoredPdfAndStatementRecordWhenExpired() throws IOException {
        Statement expired = mock(Statement.class);
        when(expired.getStorageKey()).thenReturn("statement-key");
        when(repository.findAllByUploadedAtBeforeAndStatusIn(any(), any())).thenReturn(List.of(expired));
        StatementRetentionService service = new StatementRetentionService(repository, storage, 90, true);

        service.cleanupExpiredStatements();

        verify(storage).delete("statement-key");
        verify(repository).delete(expired);
    }

    @Test
    void keepsDatabaseRecordWhenPdfDeletionFails() throws IOException {
        Statement expired = mock(Statement.class);
        when(expired.getStorageKey()).thenReturn("locked-file");
        when(repository.findAllByUploadedAtBeforeAndStatusIn(any(), any())).thenReturn(List.of(expired));
        doThrow(new IOException("storage unavailable")).when(storage).delete("locked-file");
        StatementRetentionService service = new StatementRetentionService(repository, storage, 90, true);

        service.cleanupExpiredStatements();

        verify(repository, never()).delete(expired);
    }

    @Test
    void canDisableAutomaticDeletion() {
        StatementRetentionService service = new StatementRetentionService(repository, storage, 90, false);

        service.cleanupExpiredStatements();

        verifyNoInteractions(repository, storage);
    }
}
