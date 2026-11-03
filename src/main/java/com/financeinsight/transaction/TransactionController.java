package com.financeinsight.transaction;

import com.financeinsight.account.Account;
import com.financeinsight.transaction.dto.TransactionCategoryResponse;
import com.financeinsight.transaction.dto.TransactionPageResponse;
import com.financeinsight.transaction.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public TransactionPageResponse list(
            @RequestParam(required = false) UUID statementId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) TransactionDirection direction,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Account account) {
        return transactionService.list(statementId, from, to, category, direction, q, page, size, account.getId());
    }

    @PatchMapping("/{id}/category")
    public TransactionCategoryResponse updateCategory(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateCategoryRequest request,
                                                       @AuthenticationPrincipal Account account) {
        return transactionService.updateCategory(id, request.category(), account.getId());
    }
}
