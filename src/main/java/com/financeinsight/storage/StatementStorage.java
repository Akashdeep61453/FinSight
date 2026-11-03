package com.financeinsight.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StatementStorage {
    String store(InputStream content) throws IOException;
    InputStream open(String key) throws IOException;
    void delete(String key) throws IOException;
}
