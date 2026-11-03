package com.financeinsight.transaction;

import com.financeinsight.common.exception.InvalidRequestException;
import com.financeinsight.common.exception.TransactionNotFoundException;
import com.financeinsight.transaction.dto.TransactionCategoryResponse;
import com.financeinsight.transaction.dto.TransactionPageResponse;
import com.financeinsight.transaction.dto.TransactionResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse list(UUID statementId, LocalDate from, LocalDate to,
                                        TransactionCategory category, TransactionDirection direction,
                                        String searchTerm, int page, int size, UUID ownerId) {
        if (page < 0) {
            throw new InvalidRequestException("Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (searchTerm != null && searchTerm.length() > 100) {
            throw new InvalidRequestException("Search text cannot exceed 100 characters");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("The start date must be on or before the end date");
        }

        Specification<Transaction> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statement").get("ownerId"), ownerId));
            if (statementId != null) {
                predicates.add(criteriaBuilder.equal(root.get("statement").get("id"), statementId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (direction != null) {
                predicates.add(criteriaBuilder.equal(root.get("direction"), direction));
            }
            if (searchTerm != null && !searchTerm.isBlank()) {
                String escaped = searchTerm.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
                        "%" + escaped + "%", '\\'));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id")));
        Page<Transaction> results = transactionRepository.findAll(specification, pageable);
        List<TransactionResponse> content = results.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new TransactionPageResponse(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.getTotalPages());
    }

    @Transactional
    public TransactionCategoryResponse updateCategory(UUID transactionId, TransactionCategory category, UUID ownerId) {
        Transaction transaction = transactionRepository.findByIdAndStatementOwnerId(transactionId, ownerId)
                .orElseThrow(TransactionNotFoundException::new);
        transaction.setCategory(category);
        transaction.setCategoryExplanation("Manually changed by user");
        transaction.setCategoryWasAssignedAutomatically(false);
        Transaction saved = transactionRepository.save(transaction);
        return new TransactionCategoryResponse(saved.getId(), saved.getCategory(),
                saved.isCategoryWasAssignedAutomatically());
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getStatement().getId(),
                transaction.getStatement().getCurrencyCode(), transaction.getTransactionDate(),
                transaction.getDescription(), transaction.getAmount(), transaction.getDirection(),
                transaction.getCategory(), transaction.isCategoryWasAssignedAutomatically(),
                transaction.getCategoryExplanation(), transaction.getBalanceAfter(),
                transaction.getSourcePage(), transaction.getSourceRow());
    }
}

