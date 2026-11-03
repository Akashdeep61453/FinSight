ALTER TABLE statements
    ADD COLUMN storage_key VARCHAR(255),
    ADD COLUMN parser_name VARCHAR(100),
    ADD COLUMN processing_message VARCHAR(2000),
    ADD COLUMN currency_code VARCHAR(3)
        CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$');

ALTER TABLE transactions
    ADD COLUMN category_explanation VARCHAR(255);

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS transactions_category_check;

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS chk_uncategorized_manual;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_category CHECK (category IN (
        'INCOME', 'HOUSING', 'GROCERIES', 'TRANSPORT', 'UTILITIES',
        'HEALTHCARE', 'SHOPPING', 'DINING', 'TRANSFER', 'REFUND', 'FEES',
        'OTHER', 'UNCATEGORIZED'
    ));
