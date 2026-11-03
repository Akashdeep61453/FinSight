CREATE TABLE statements (
    id UUID PRIMARY KEY,
    owner_id UUID,
    original_filename VARCHAR(255) NOT NULL,
    bank_name VARCHAR(120),
    uploaded_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT chk_statements_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'COMPLETED', 'NEEDS_REVIEW', 'FAILED'))
);

CREATE INDEX idx_statements_owner_uploaded ON statements (owner_id, uploaded_at DESC);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    statement_id UUID NOT NULL REFERENCES statements (id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount >= 0),
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    category VARCHAR(30) NOT NULL CHECK (category IN (
        'INCOME', 'HOUSING', 'GROCERIES', 'TRANSPORT', 'UTILITIES',
        'HEALTHCARE', 'SHOPPING', 'DINING', 'TRANSFER', 'FEES',
        'OTHER', 'UNCATEGORIZED'
    )),
    category_was_assigned_automatically BOOLEAN NOT NULL DEFAULT TRUE,
    balance_after NUMERIC(19, 4),
    source_page INTEGER CHECK (source_page IS NULL OR source_page > 0),
    source_row INTEGER CHECK (source_row IS NULL OR source_row > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_statement_date ON transactions (statement_id, transaction_date);
CREATE INDEX idx_transactions_category_date ON transactions (category, transaction_date);
