package com.financeinsight.transaction;

import com.financeinsight.common.exception.TransactionNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionOwnershipTest {
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionService service = new TransactionService(repository);

    @Test
    void transactionListAddsAuthenticatedOwnerToItsDatabasePredicate() {
        UUID ownerId = UUID.randomUUID();
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.list(null, null, null, null, null, null, 0, 20, ownerId);

        ArgumentCaptor<Specification<Transaction>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), any(Pageable.class));
        @SuppressWarnings("unchecked") Root<Transaction> root = mock(Root.class);
        @SuppressWarnings("unchecked") Path<Object> statementPath = mock(Path.class);
        @SuppressWarnings("unchecked") Path<Object> ownerPath = mock(Path.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("statement")).thenReturn(statementPath);
        when(statementPath.get("ownerId")).thenReturn(ownerPath);
        when(builder.equal(ownerPath, ownerId)).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        captor.getValue().toPredicate(root, null, builder);

        verify(builder).equal(ownerPath, ownerId);
    }

    @Test
    void categoryUpdateLooksUpTransactionThroughItsOwningAccount() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(repository.findByIdAndStatementOwnerId(id, ownerId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> service.updateCategory(id, TransactionCategory.GROCERIES, ownerId));
        verify(repository, never()).findById(id);
    }
}
