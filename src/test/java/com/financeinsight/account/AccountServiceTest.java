package com.financeinsight.account;

import com.financeinsight.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final AccountRepository repository = mock(AccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AccountService service = new AccountService(repository, encoder);

    @Test
    void registrationHashesPasswordAndAssignsLegacyRowsToFirstAccount() {
        Account saved = mock(Account.class);
        when(repository.count()).thenReturn(0L);
        when(repository.saveAndFlush(any(Account.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(UUID.randomUUID());
        when(encoder.encode("long-enough-password")).thenReturn("encoded-hash");

        service.register("first.user", "long-enough-password");

        verify(repository).lockRegistration();
        verify(repository).claimLegacyStatements(saved.getId());
        verify(encoder).encode("long-enough-password");
    }

    @Test
    void laterAccountsDoNotClaimExistingStatements() {
        when(repository.count()).thenReturn(1L);
        when(repository.existsByUsername("second.user")).thenReturn(false);
        when(repository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(any())).thenReturn("encoded-hash");

        service.register("second.user", "long-enough-password");

        verify(repository, never()).claimLegacyStatements(any());
    }

    @Test
    void enforcesStrongPasswordAndSafeUsername() {
        assertThrows(InvalidRequestException.class, () -> service.register("ab", "long-enough-password"));
        assertThrows(InvalidRequestException.class, () -> service.register("valid.user", "too-short"));
        verifyNoInteractions(repository);
    }
}
