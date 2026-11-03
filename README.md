# Bank Statement Analyzer

A personal finance application that turns bank statement PDFs into structured transactions, categorized spending insights, savings trends, and an income-versus-expense overview.

## Current progress

- **Phase 1 — Project foundation:** complete. Java 21 / Spring Boot backend skeleton, PostgreSQL and Flyway configuration, PDFBox dependency, and synthetic fixture are in place. The Java backend compiles successfully with Maven.
- **Phase 2 — Data model and database schema:** implemented. Statement and transaction entities, enums, repositories, and the initial Flyway migration are in place.
- **Phase 3 — PDF upload:** implemented. Upload validation, local file storage, statement record creation, and status lookup are in place.
- **Phase 4 — PDF text extraction:** implemented. PDFBox extracts text page by page from stored statements and reports invalid, password-protected, inaccessible, and textless PDFs distinctly.
- **Phase 5 — Parsing and normalization:** implemented for the synthetic fixture layout. A parser interface, router, normalized transaction records, row-level warnings, and explicit unknown-format behavior are in place.
- **Phase 6 — Categorization:** implemented with transparent rules and a manual category correction endpoint.
- **Phase 7 — Processing workflow:** implemented synchronously on upload. Statements move through processing states; parsed transactions and category suggestions are persisted together, with review warnings and failures recorded.
- **Phase 8 — Transaction API:** implemented with pagination and filters for statement, date range, category, direction, and description search.
- **Phase 9 — Analytics API:** implemented with currency-separated summaries, monthly trends, and category spending.
- **Phase 10 — React dashboard:** implemented and production build verified. The frontend includes PDF upload, date filters, currency-aware summary cards/charts, searchable transactions, pagination, and category editing.
- **Phase 11 — Parser and API coverage:** implemented. Twenty-four parser, categorization, analytics, REST, account, ownership, and retention tests pass.
- **Phase 12 — Security, privacy, and deployment:** implemented for local and single-node Compose deployment. Users can register; statement, transaction, and analytics access is scoped to the authenticated account. Source PDFs are automatically removed after the configured retention period (365 days by default). The production Compose overlay adds Caddy-managed HTTPS. Backups remain an operator responsibility.
- **Phase 13:** planned in the [development roadmap](#development-phases).

The initial fixture is deliberately bank-neutral. The sample parser reads its currency code, and analytics keep totals grouped by currency so different denominations are never summed together. No real bank format has been selected or claimed as supported yet. The parser contract normalizes supported transaction rows to date, description, amount, debit/credit direction, optional balance, and source page/row. Before implementing the first bank-specific parser, choose a real statement format and provide a sanitized sample; the included fixture is synthetic and only useful for development scaffolding.

## Technology Stack

- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Bean Validation
- **PDF processing:** Apache PDFBox
- **Database:** PostgreSQL, with Flyway migrations
- **Frontend:** React 19.3 and Vite 8
- **Testing:** JUnit, Mockito, and Spring Boot Test

## Prerequisites

- Java 21
- Maven 3.6.3 or later
- PostgreSQL 15 or later
- Node.js 20.19+ (or 22.12+) and npm

## Run locally

1. Create a PostgreSQL database named `finance_insight` and a user with credentials matching the defaults below, or set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
2. Start the backend:

   ```powershell
   mvn spring-boot:run
   ```

The backend listens on `http://localhost:8080`. Flyway creates the schema on startup. A reachable PostgreSQL database is required. Uploaded PDFs are stored under `./uploads` by default; set `STATEMENT_STORAGE_DIRECTORY` to change that location.

Start the React dashboard in a separate terminal:

```powershell
cd frontend
npm install
npm run dev
```

Vite serves the frontend at `http://localhost:5173` and proxies `/api` requests to the backend. Set `VITE_API_BASE_URL` when the API is hosted elsewhere.

The dashboard opens to a sign-in screen with an account registration option. Passwords are stored as BCrypt hashes. Auth credentials remain in browser memory for the current tab session. HTTP Basic credentials must only be used over a trusted network or HTTPS.

Run the Java test suite from the project root with `mvn test`.

## Docker deployment baseline

Install Docker Engine with Docker Compose v2.24 or later. Set secrets in the shell or in a local `.env` file (which is ignored by Git):

```dotenv
DATABASE_PASSWORD=replace-with-a-long-random-database-secret
WEB_PORT=8081
```

Start the local stack with `docker compose up --build -d`; the dashboard is served at `http://localhost:8081`. Register your first user in the dashboard. The backend and database are not published directly to the host. PostgreSQL data and uploaded source PDFs live in separate named volumes. Spring Boot applies Flyway migrations on startup. `/actuator/health` is public and returns basic health only; readiness and liveness probes are available under `/actuator/health/`.

For a public single-node deployment, set `PUBLIC_DOMAIN` to a DNS name pointing at the host, then run `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile production up --build -d`. The overlay unpublishes the plain HTTP frontend port and exposes Caddy on ports 80 and 443. Caddy obtains and renews the HTTPS certificate automatically; those ports must be reachable from the internet. Set a strong `DATABASE_PASSWORD` in the deployment secret manager. User passwords are created through the registration form and hashed in PostgreSQL. The first registered account adopts any legacy statements whose owner was not assigned; subsequent accounts cannot access another account's records.

### Back up and restore

Back up the PostgreSQL database and `statement-files` volume together, encrypt the backup at rest, and restrict access because the volume contains original bank PDFs. For a database dump:

```sh
docker compose exec -T database pg_dump -U finance_insight -d finance_insight -Fc > finance_insight.dump
```

Restore into a stopped or fresh stack's database with:

```sh
docker compose exec -T database pg_restore -U finance_insight -d finance_insight --clean --if-exists < finance_insight.dump
```

Also archive and restore the `statement-files` Docker volume using your host's Docker volume backup procedure, then verify both the application and uploaded files before returning service. Back up the `caddy-data` volume if you need to preserve Caddy's certificate/account state; certificates can also be reissued. Schedule backups and test restores according to your recovery needs; the application does not schedule backups itself.

### Source PDF retention and logs

Source PDFs are retained for reprocessing and automatically deleted with their statement and transactions after 365 days by default. Set `PDF_RETENTION_DAYS` to a positive number and `PDF_RETENTION_ENABLED=false` to disable cleanup. The cleanup runs daily at 02:30 server time by default; set `PDF_RETENTION_CRON` to change it. Keep backups subject to an equally clear retention policy. Processing logs record statement IDs and exception classes, not extracted statement text; do not enable HTTP request-body or PDF content logging.

## Development Phases

### Phase 1 — Define scope and create the project

1. Confirm the first supported bank statement format and define the transaction fields the system must extract.
2. Decide initial categories, such as income, housing, groceries, transport, utilities, shopping, and other.
3. Create the Spring Boot backend with Java 21 and the required dependencies.
4. Create the React frontend separately, or place it in a `frontend/` directory in the same repository.
5. Add development configuration for PostgreSQL and environment variables for credentials.
6. Add a short sample statement fixture using synthetic or anonymized data.

**Deliverable:** A runnable backend and frontend skeleton with a documented first statement format.

### Phase 2 — Model data and initialize the database

1. Create a `Statement` model with owner, original filename, detected bank, upload time, and processing status.
2. Create a `Transaction` model with statement reference, date, description, amount, debit/credit direction, and category.
3. Use `BigDecimal` for monetary values and `LocalDate` for transaction dates.
4. Create Flyway migrations for tables, constraints, and useful indexes.
5. Add JPA repositories for statements and transactions.
6. Define statement statuses such as `UPLOADED`, `PROCESSING`, `COMPLETED`, `NEEDS_REVIEW`, and `FAILED`.

**Deliverable:** The application can store and retrieve statement and transaction records.

### Phase 3 — Accept PDF uploads safely

1. Add a statement upload endpoint, for example `POST /api/statements`.
2. Validate file size, filename, and supported media type.
3. Store the uploaded file in configured local development storage; keep storage behind an interface so it can move to object storage later.
4. Create a statement record and return its ID and initial status.
5. Add consistent API error responses for invalid, empty, encrypted, or unreadable PDFs.
6. Avoid logging statement text or other sensitive financial details.

**Deliverable:** A user can upload a PDF and receive a statement ID and the result of synchronous processing.

### Phase 4 — Extract PDF text

1. Implement `PdfTextExtractor` using Apache PDFBox.
2. Extract page text while retaining page boundaries where useful for parsing.
3. Detect PDFs that contain no usable text and report them as unsupported or needing review.
4. Handle encrypted, corrupt, and unexpectedly large documents gracefully.
5. Keep PDF extraction independent of bank-specific row parsing.

**Deliverable:** Uploaded text-based PDFs can be converted into extracted text for parsing.

### Phase 5 — Parse and normalize transactions

1. Define a `StatementParser` interface that accepts extracted statement content and returns normalized transaction rows.
2. Implement the first bank-specific parser using the chosen sample format.
3. Parse transaction dates, descriptions, debit/credit amounts, and balances when available.
4. Normalize date formats, number separators, currency symbols, and debit/credit conventions.
5. Add validation for missing dates, invalid amounts, repeated headers, and summary rows.
6. Preserve source page or row information where possible to help users review a parsed transaction.
7. Add a `ParserRouter` that selects a parser using known bank names, headings, or layout clues.
8. Mark statements `NEEDS_REVIEW` when the format is unknown or parsing confidence is low.

**Deliverable:** The first supported statement format produces consistent transaction records.

### Phase 6 — Categorize transactions

1. Define a category enum or database-backed category model.
2. Create a `Categorizer` interface.
3. Implement a rules-based categorizer using merchant names and description keywords.
4. Assign an `OTHER` or `UNCATEGORIZED` category when no rule matches.
5. Save the assigned category and whether it was assigned automatically.
6. Add an endpoint to update a transaction category, for example `PATCH /api/transactions/{id}/category`.
7. Keep user corrections so they can improve future rules or train a later classifier.

**Deliverable:** Transactions receive explainable default categories and can be corrected by users.

### Phase 7 — Complete the statement processing workflow

1. Coordinate upload, extraction, parser selection, normalization, categorization, and database persistence in `StatementService`.
2. Update statement status as processing advances.
3. Avoid saving partial or duplicate transaction sets if processing fails partway through.
4. Make processing repeatable or safely retryable without duplicating transactions.
5. Store useful failure reasons and parsing warnings without exposing sensitive content.
6. Add `GET /api/statements/{id}` so the frontend can check status and review warnings.

**Deliverable:** An uploaded statement follows a complete, observable processing lifecycle.

### Phase 8 — Build transaction APIs

1. Add `GET /api/transactions` with pagination (default 20 rows; maximum 100 per page).
2. Support filters for statement, date range, category, debit/credit direction, and description search.
3. Return DTOs rather than exposing database entities directly.
4. Add transaction detail and category correction endpoints.
5. Validate filter ranges and return clear errors for invalid requests. Example: `/api/transactions?from=2026-01-01&to=2026-01-31&category=GROCERIES&direction=DEBIT&page=0&size=20`.

**Deliverable:** The frontend can browse, filter, and correct imported transactions.

### Phase 9 — Build analytics APIs

1. Add a summary endpoint such as `GET /api/analytics/summary`.
2. Calculate income and expenses over a selected date range.
3. Calculate net savings as income minus expenses.
4. Calculate savings rate only when income is greater than zero.
5. Add monthly income and expense totals for trend charts.
6. Add spending totals grouped by category.
7. Exclude transfers from income and expenses, count recognized refunds as reductions to expenses, and keep unknown credits out of income until reviewed.
8. Document the health-score formula and its inputs before showing it in the UI.

**Metric rules:** income includes only categorized income credits; expenses include debit transactions except transfers; recognized credit refunds reduce net expenses; transfers and unknown credits are excluded from income/expense totals. Analytics are grouped by statement currency. The default window is the latest 12 months, and requests are limited to 120 months.

**Deliverable:** The API returns dashboard-ready spending, cash-flow, and savings summaries.

### Phase 10 — Build the React dashboard

1. Add a PDF upload page with file validation and upload progress.
2. Show statement processing status and parsing warnings.
3. Create a transaction table with date, description, amount, direction, and category.
4. Add transaction filters and category editing.
5. Show income-versus-expense totals and net savings for a selected period.
6. Add monthly trend charts and a spending-by-category chart.
7. Explain the selected date range and definitions behind each metric.
8. Handle empty data, loading states, API failures, and statements needing review.

**Deliverable:** Users can upload a statement and understand the resulting transactions and trends.

### Phase 11 — Test and improve parser coverage

1. Unit test each parser with representative input and expected transactions.
2. Test malformed rows, multiple date formats, negative amounts, and debit/credit columns.
3. Test categorization rules and user corrections.
4. Test analytics calculations against known example data.
5. Add API tests for upload validation, processing status, filtering, and error responses.
6. Add additional bank parsers one at a time, using anonymized fixtures.
7. Track parse failures and warnings so the next parser improvement is evidence-based.

**Deliverable:** Supported formats and core calculations have repeatable automated coverage.

### Phase 12 — Security, privacy, and deployment

1. **Implemented:** account registration uses BCrypt password hashes, Basic authentication, and account ownership checks for statement access, transaction lists and edits, and analytics. The first account claims old unowned records once.
2. **Implemented deployment path:** the production Compose overlay routes through Caddy for automatic HTTPS; secrets come from environment variables or the deployment secret manager.
3. **Implemented:** source PDFs are retained for 365 days by default, configurable through environment variables.
4. **Implemented:** a daily cleanup removes expired PDFs, statements, and their transactions; processing logs exclude statement contents.
5. **Implemented:** Dockerfiles and Compose configuration containerize the backend, frontend, and PostgreSQL; the production overlay also adds the TLS reverse proxy.
6. **Implemented:** credentials and database connection settings are externalized; Flyway applies migrations at startup.
7. **Implemented:** container health checks and Spring Boot readiness/liveness probes are configured.
8. **Documented:** database backup and restore commands are provided; operators must also back up the PDF and Caddy certificate volumes.

**Deliverable:** A multi-account stack can be deployed locally or behind automatically managed HTTPS, with account-scoped data, configurable PDF retention, and health probes. Backup scheduling, restore drills, host security, and secret storage remain operator responsibilities.

### Phase 13 — Optional general parser and classifier

1. Collect diverse, anonymized statement layouts and label their transaction rows.
2. Measure parser accuracy and identify formats that need explicit bank parsers.
3. Add layout detection as a fallback for formats not covered by a known parser.
4. Train and evaluate a transaction categorization classifier against a held-out dataset.
5. Keep a confidence threshold and allow low-confidence predictions to be reviewed.
6. Retain rule-based categorization as a transparent fallback.

**Deliverable:** Broader format support and improved categorization, backed by measured results.

## Suggested Backend Package Layout

```text
src/main/java/com/financeinsight/
├── FinanceInsightApplication.java
├── common/
│   ├── exception/
│   └── response/
├── config/
├── statement/
│   ├── Statement.java
│   ├── StatementController.java
│   ├── StatementRepository.java
│   ├── StatementService.java
│   └── dto/
├── parser/
│   ├── StatementParser.java
│   ├── ParserRouter.java
│   ├── PdfTextExtractor.java
│   └── bank/
├── transaction/
│   ├── Transaction.java
│   ├── TransactionRepository.java
│   ├── TransactionService.java
│   └── dto/
├── categorization/
│   ├── Category.java
│   ├── Categorizer.java
│   └── RuleBasedCategorizer.java
└── analytics/
    ├── AnalyticsController.java
    ├── AnalyticsService.java
    └── dto/
```

## Initial API Sketch

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/statements` | Upload a PDF statement |
| `GET` | `/api/statements/{id}` | Get processing status and warnings |
| `GET` | `/api/transactions` | List and filter transactions (`statementId`, `from`, `to`, `category`, `direction`, `q`, `page`, `size`) |
| `PATCH` | `/api/transactions/{id}/category` | Correct a transaction category |
| `GET` | `/api/analytics/summary` | Get currency-separated income, expenses, refunds, and savings totals |
| `GET` | `/api/analytics/trends` | Get currency-separated monthly income and expense totals |
| `GET` | `/api/analytics/categories` | Get spending totals by category |

## Development Principles

- Normalize all supported bank formats into one transaction model.
- Keep PDF extraction, parsing, categorization, persistence, and analytics as separate responsibilities.
- Treat uncertain parses as reviewable results instead of silently accepting them.
- Use synthetic or anonymized statement samples in source control.
- Keep calculations and category rules clear enough to explain to users.


















