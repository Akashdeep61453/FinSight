package com.financeinsight.config;

import com.financeinsight.storage.LocalStatementStorage;
import com.financeinsight.storage.StatementStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class StorageConfig {

    @Bean
    StatementStorage statementStorage(@Value("${app.storage.local-directory:./uploads}") String directory) {
        return new LocalStatementStorage(Path.of(directory));
    }
}
