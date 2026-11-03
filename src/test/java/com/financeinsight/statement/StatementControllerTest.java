package com.financeinsight.statement;

import com.financeinsight.account.Account;
import com.financeinsight.config.SecurityConfig;
import com.financeinsight.statement.dto.StatementStatusResponse;
import com.financeinsight.statement.dto.StatementUploadResponse;
import com.financeinsight.statement.StatementStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementController.class)
@Import(SecurityConfig.class)
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatementService statementService;

    @Test
    void rejectsUnauthenticatedStatementRequests() throws Exception {
        mockMvc.perform(get("/api/statements/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .doesNotExist("WWW-Authenticate"));
    }

    @Test
    void acceptsPdfUploadAndReturnsProcessingResult() throws Exception {
        UUID id = UUID.randomUUID();
        when(statementService.upload(any(), any())).thenReturn(new StatementUploadResponse(id, "sample.pdf", null,
                "INR", StatementStatus.COMPLETED, Instant.parse("2026-01-01T10:00:00Z"),
                "synthetic-sample-v1", null));
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7".getBytes());

        mockMvc.perform(multipart("/api/statements").file(file).with(user(new Account("tester", "hash"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currencyCode").value("INR"));
    }

    @Test
    void returnsStatementProcessingStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(statementService.getStatus(eq(id), any())).thenReturn(new StatementStatusResponse(id, "sample.pdf", null,
                "INR", StatementStatus.NEEDS_REVIEW, Instant.parse("2026-01-01T10:00:00Z"),
                null, "This statement format is not recognized."));

        mockMvc.perform(get("/api/statements/{id}", id).with(user(new Account("tester", "hash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.processingMessage").value("This statement format is not recognized."));
    }
}
