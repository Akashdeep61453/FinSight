package com.financeinsight.transaction;

import com.financeinsight.account.Account;
import com.financeinsight.config.SecurityConfig;
import com.financeinsight.transaction.dto.TransactionCategoryResponse;
import com.financeinsight.transaction.dto.TransactionPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void bindsTransactionFiltersAndReturnsPagedResults() throws Exception {
        LocalDate from = LocalDate.parse("2026-01-01");
        LocalDate to = LocalDate.parse("2026-01-31");
        when(transactionService.list(null, from, to, TransactionCategory.GROCERIES,
                TransactionDirection.DEBIT, "market", 1, 10, null))
                .thenReturn(new TransactionPageResponse(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/transactions")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("category", "GROCERIES")
                        .param("direction", "DEBIT")
                        .param("q", "market")
                        .param("page", "1")
                        .param("size", "10")
                        .with(user(new Account("tester", "hash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content").isArray());

        verify(transactionService).list(null, from, to, TransactionCategory.GROCERIES,
                TransactionDirection.DEBIT, "market", 1, 10, null);
    }

    @Test
    void allowsManualCategoryCorrection() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.updateCategory(id, TransactionCategory.GROCERIES, null))
                .thenReturn(new TransactionCategoryResponse(id, TransactionCategory.GROCERIES, false));

        mockMvc.perform(patch("/api/transactions/{id}/category", id)
                        .contentType("application/json")
                        .content("{\"category\":\"GROCERIES\"}")
                        .with(user(new Account("tester", "hash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.categoryWasAssignedAutomatically").value(false));
    }
}
