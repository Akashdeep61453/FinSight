package com.financeinsight.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class LocalStatementStorage implements StatementStorage {

    private final Path root;

    public LocalStatementStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream content) throws IOException {
        Files.createDirectories(root);
        String key = UUID.randomUUID() + ".pdf";
        Path destination = resolveKey(key);
        Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolveKey(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolveKey(key));
    }

    private Path resolveKey(String key) throws IOException {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.pdf")) {
            throw new IOException("Invalid statement storage key");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.getParent().equals(root)) {
            throw new IOException("Invalid statement storage key");
        }
        return resolved;
    }
}
