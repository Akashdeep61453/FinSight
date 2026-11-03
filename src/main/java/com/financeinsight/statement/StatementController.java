package com.financeinsight.statement;

import com.financeinsight.account.Account;
import com.financeinsight.statement.dto.StatementStatusResponse;
import com.financeinsight.statement.dto.StatementUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                           @AuthenticationPrincipal Account account) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statementService.upload(file, account.getId()));
    }

    @GetMapping("/{id}")
    public StatementStatusResponse getStatus(@PathVariable UUID id, @AuthenticationPrincipal Account account) {
        return statementService.getStatus(id, account.getId());
    }
}
